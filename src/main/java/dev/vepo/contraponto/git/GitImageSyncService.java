package dev.vepo.contraponto.git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.image.ContentImageMarkerService;
import dev.vepo.contraponto.image.Image;
import dev.vepo.contraponto.image.ImageRepository;
import dev.vepo.contraponto.image.ImageService;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.renderer.Format;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class GitImageSyncService {

    private static final Logger LOG = LoggerFactory.getLogger(GitImageSyncService.class);

    private static final Pattern ASCIIDOC_IMAGE =
            Pattern.compile("image::?([^\\s\\[]+)\\[", Pattern.CASE_INSENSITIVE);

    private static String contentTypeForExt(String ext) {
        return ImageService.contentTypeForExtension(ext);
    }

    private static String escapeMarkdownAlt(String alt) {
        return alt.replace("]", "\\]");
    }

    private static String extensionFromUrl(String url) {
        int dot = url.lastIndexOf('.');
        return dot >= 0 ? url.substring(dot) : ".png";
    }

    private static boolean isImageExtension(String ext) {
        return switch (ext.toLowerCase(Locale.ROOT)) {
            case ".png", ".jpg", ".jpeg", ".gif", ".webp", ".avif", ".svg" -> true;
            default -> false;
        };
    }

    private static String relativeAssetPath(JekyllLayoutConvention convention, Image image) {
        String ext = extensionFromUrl(image.getUrl());
        String gitPath = image.getGitAssetRelativePath();
        if (gitPath != null && !gitPath.isBlank()) {
            return "%s/%s%s".formatted(convention.assetsRelative(), gitPath, ext);
        }
        return "%s/%s%s".formatted(convention.assetsRelative(), image.getUuid(), ext);
    }

    public static Pattern relativeAssetPattern(JekyllLayoutConvention convention) {
        String dir = Pattern.quote(convention.assetsRelative());
        // Optional slash after ../ so "(/assets/images/…)" is matched as a whole, not
        // as a partial match against "/api/images/…".
        return Pattern.compile("(?:\\.\\./)*/?%s/((?:[A-Za-z0-9._-]+/)*[A-Za-z0-9._-]+)(\\.[A-Za-z0-9]{2,8})".formatted(dir));
    }

    private static Path resolveAssetFile(Path assetsDir, String assetRelativePath, String ext) {
        Path exact = assetsDir.resolve(assetRelativePath + ext);
        if (Files.isRegularFile(exact)) {
            return exact;
        }
        Path assetPath = assetsDir.resolve(assetRelativePath);
        String basename = GitFrontMatterResolver.assetBasename(assetRelativePath);
        Path dir = assetPath.getParent() != null ? assetPath.getParent() : assetsDir;
        String extLower = ext.toLowerCase(Locale.ROOT);
        try (Stream<Path> listing = Files.list(dir)) {
            return listing
                          .filter(Files::isRegularFile)
                          .filter(p -> {
                              String name = p.getFileName().toString();
                              int dot = name.lastIndexOf('.');
                              if (dot <= 0) {
                                  return false;
                              }
                              String base = name.substring(0, dot);
                              String fileExt = name.substring(dot).toLowerCase(Locale.ROOT);
                              return base.equalsIgnoreCase(basename) && fileExt.equals(extLower);
                          })
                          .findFirst()
                          .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    private static String uuidFromUrl(String url) {
        Matcher api = ContentImageMarkerService.IMAGE_URL.matcher(url);
        if (api.find()) {
            return api.group(1);
        }
        return null;
    }

    private final ImageRepository imageRepository;

    private final ImageService imageService;

    private final ContentImageMarkerService markerService;

    @Inject
    public GitImageSyncService(ImageRepository imageRepository,
                               ImageService imageService,
                               ContentImageMarkerService markerService) {
        this.imageRepository = imageRepository;
        this.imageService = imageService;
        this.markerService = markerService;
    }

    public void addCoverFrontMatter(Map<String, Object> fm, Post post, JekyllLayoutConvention convention) {
        Image cover = post.getCover();
        if (cover == null && post.getLivePublication() != null) {
            cover = post.getLivePublication().getCover();
        }
        if (cover != null) {
            fm.put("cover", relativeAssetPath(convention, cover));
        }
    }

    private void copyImagesToRepo(Git git, Path workspace, JekyllLayoutConvention convention, Set<String> uuids)
            throws IOException {
        Path assetsDir = convention.resolveAssets(workspace);
        Files.createDirectories(assetsDir);
        Path repoRoot = workspace.toAbsolutePath().normalize();
        for (String uuid : uuids) {
            imageRepository.findByUuid(uuid).ifPresent(image -> {
                try {
                    String ext = extensionFromUrl(image.getUrl());
                    String destKey = image.getGitAssetRelativePath() != null && !image.getGitAssetRelativePath().isBlank()
                                                                                                                           ? image.getGitAssetRelativePath()
                                                                                                                                   + ext
                                                                                                                           : uuid + ext;
                    Path dest = assetsDir.resolve(destKey);
                    Files.createDirectories(dest.getParent() != null ? dest.getParent() : assetsDir);
                    var imageData = imageService.getImage(image.getFilename());
                    Files.write(dest, imageData.data());
                    String rel = repoRoot.relativize(dest.toAbsolutePath().normalize()).toString().replace('\\', '/');
                    git.add().addFilepattern(rel).call();
                } catch (IOException | GitAPIException e) {
                    LOG.warn("Failed to export image uuid={}", uuid, e);
                }
            });
        }
    }

    private void ensureImageInDb(Blog blog,
                                 String uuid,
                                 String ext,
                                 Path source,
                                 String gitAssetRelativePath) {
        if (!Files.exists(source)) {
            return;
        }
        var existing = imageRepository.findByUuid(uuid);
        if (existing.isPresent()) {
            Image image = existing.get();
            if ((image.getGitAssetRelativePath() == null || image.getGitAssetRelativePath().isBlank())
                    && gitAssetRelativePath != null && !gitAssetRelativePath.isBlank()) {
                image.setGitAssetRelativePath(gitAssetRelativePath);
                imageRepository.update(image);
            }
            return;
        }
        try {
            byte[] content = Files.readAllBytes(source);
            imageService.storeImportedImage(blog, uuid, ext, content, contentTypeForExt(ext), gitAssetRelativePath);
        } catch (IOException e) {
            LOG.warn("Failed to import image uuid={}", uuid, e);
        }
    }

    public void exportImagesForPost(Git git,
                                    Path workspace,
                                    JekyllLayoutConvention convention,
                                    Post post,
                                    String exportBody)
            throws IOException, GitAPIException {
        Set<String> uuids = new LinkedHashSet<>(markerService.extractImageUuids(exportBody));
        if (post.getCover() != null) {
            uuids.add(post.getCover().getUuid());
        }
        copyImagesToRepo(git, workspace, convention, uuids);
    }

    void importAssetFromRelativePath(Blog blog,
                                     Path workspace,
                                     JekyllLayoutConvention convention,
                                     String assetRelativePath,
                                     String ext) {
        Path source = resolveAssetFile(convention.resolveAssets(workspace), assetRelativePath, ext);
        if (source == null) {
            LOG.debug("Asset not found for import: {}{}", assetRelativePath, ext);
            return;
        }
        String basename = GitFrontMatterResolver.assetBasename(assetRelativePath);
        String uuid = GitImportedAssetId.normalize(basename, ext);
        ensureImageInDb(blog, uuid, ext, source, assetRelativePath);
    }

    @Transactional
    public void importAssetsFromWorkspace(Blog blog, Path workspace, JekyllLayoutConvention convention) throws IOException {
        Path assetsDir = convention.resolveAssets(workspace);
        if (!Files.isDirectory(assetsDir)) {
            return;
        }
        try (Stream<Path> files = Files.walk(assetsDir)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                String name = file.getFileName().toString();
                int dot = name.lastIndexOf('.');
                if (dot <= 0) {
                    continue;
                }
                String fileExt = name.substring(dot);
                if (!isImageExtension(fileExt)) {
                    continue;
                }
                Path rel = assetsDir.relativize(file);
                String assetRel = rel.toString().replace('\\', '/');
                int extDot = assetRel.lastIndexOf('.');
                if (extDot <= 0) {
                    continue;
                }
                String assetKey = assetRel.substring(0, extDot);
                String uuid = GitImportedAssetId.normalize(GitFrontMatterResolver.assetBasename(assetKey), fileExt);
                ensureImageInDb(blog, uuid, fileExt, file, assetKey);
            }
        }
    }

    /**
     * Imports only assets referenced by this post (cover + body), not the whole
     * tree.
     */
    public void importImagesForPost(Blog blog,
                                    Path workspace,
                                    JekyllLayoutConvention convention,
                                    String coverPath,
                                    String body,
                                    Format format) {
        Set<String> assetPaths = GitPostAssetReferences.collectAssetRelativePaths(coverPath, body, format, convention);
        for (String assetRelWithExt : assetPaths) {
            String assetRel = GitPostAssetReferences.basenameWithoutExtension(assetRelWithExt);
            String ext = GitPostAssetReferences.extensionWithDot(assetRelWithExt);
            importAssetFromRelativePath(blog, workspace, convention, assetRel, ext);
        }
    }

    public String prepareBodyForExport(String body, JekyllLayoutConvention convention) {
        if (body == null) {
            return "";
        }
        String stripped = markerService.stripMarkersForExport(body);
        Matcher matcher = ContentImageMarkerService.IMAGE_URL.matcher(stripped);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String uuid = matcher.group(1);
            String ext = extensionFromUrl(matcher.group(0));
            matcher.appendReplacement(sb, Matcher.quoteReplacement(relativeAssetPathForUuid(convention, uuid, ext)));
        }
        matcher.appendTail(sb);
        return rewriteMarkdownAlts(sb.toString());
    }

    public String prepareBodyForImport(String body,
                                       Blog blog,
                                       Path workspace,
                                       JekyllLayoutConvention convention,
                                       Format format) {
        if (body == null) {
            return "";
        }
        String rewritten = rewriteMarkdownAssetUrls(body, blog, workspace, convention);
        if (format == Format.ASCIIDOC) {
            rewritten = rewriteAsciiDocAssetUrls(rewritten, blog, workspace, convention);
        }
        return markerService.toStoredContent(rewritten);
    }

    private String relativeAssetPathForUuid(JekyllLayoutConvention convention, String uuid, String ext) {
        return imageRepository.findByUuid(uuid)
                              .map(img -> relativeAssetPath(convention, img))
                              .orElse("%s/%s%s".formatted(convention.assetsRelative(), uuid, ext));
    }

    private String resolveAsciiDocImageReplacement(String rawPath,
                                                   Blog blog,
                                                   Path workspace,
                                                   JekyllLayoutConvention convention) {
        Set<String> normalized = GitPostAssetReferences.collectAssetRelativePaths(rawPath, "", Format.MARKDOWN, convention);
        if (normalized.isEmpty()) {
            return null;
        }
        String assetRelWithExt = normalized.iterator().next();
        String assetRel = GitPostAssetReferences.basenameWithoutExtension(assetRelWithExt);
        String ext = GitPostAssetReferences.extensionWithDot(assetRelWithExt);
        importAssetFromRelativePath(blog, workspace, convention, assetRel, ext);
        String uuid = GitImportedAssetId.normalize(GitFrontMatterResolver.assetBasename(assetRel), ext);
        return "/api/images/%s%s".formatted(uuid, ext);
    }

    private String rewriteAsciiDocAssetUrls(String body,
                                            Blog blog,
                                            Path workspace,
                                            JekyllLayoutConvention convention) {
        Matcher m = ASCIIDOC_IMAGE.matcher(body);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String rawPath = m.group(1).strip();
            String replacement = resolveAsciiDocImageReplacement(rawPath, blog, workspace, convention);
            if (replacement == null) {
                m.appendReplacement(sb, Matcher.quoteReplacement(m.group(0)));
            } else {
                m.appendReplacement(sb, Matcher.quoteReplacement("image::%s[".formatted(replacement)));
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String rewriteMarkdownAlts(String content) {
        Matcher m = Pattern.compile("!\\[([^\\]]*)\\]\\(([^)]+)\\)").matcher(content);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String url = m.group(2);
            String uuid = uuidFromUrl(url);
            if (uuid == null) {
                m.appendReplacement(sb, Matcher.quoteReplacement(m.group(0)));
                continue;
            }
            String alt = imageRepository.findByUuid(uuid).map(img -> {
                if (img.getAltText() != null && !img.getAltText().isBlank()) {
                    return img.getAltText();
                }
                return m.group(1);
            }).orElse(m.group(1));
            m.appendReplacement(sb, Matcher.quoteReplacement("![%s](%s)".formatted(escapeMarkdownAlt(alt), url)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String rewriteMarkdownAssetUrls(String body,
                                            Blog blog,
                                            Path workspace,
                                            JekyllLayoutConvention convention) {
        Matcher m = relativeAssetPattern(convention).matcher(body);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String assetRel = m.group(1);
            String ext = m.group(2);
            String basename = GitFrontMatterResolver.assetBasename(assetRel);
            String uuid = GitImportedAssetId.normalize(basename, ext);
            importAssetFromRelativePath(blog, workspace, convention, assetRel, ext);
            m.appendReplacement(sb, Matcher.quoteReplacement("/api/images/%s%s".formatted(uuid, ext)));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
