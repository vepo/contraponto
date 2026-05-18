package dev.vepo.contraponto.git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Locale;
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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class GitImageSyncService {

    private static final Logger LOG = LoggerFactory.getLogger(GitImageSyncService.class);

    private static String contentTypeForExt(String ext) {
        return switch (ext.toLowerCase(Locale.ROOT)) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".gif" -> "image/gif";
            case ".webp" -> "image/webp";
            default -> "image/png";
        };
    }

    private static String escapeMarkdownAlt(String alt) {
        return alt.replace("]", "\\]");
    }

    private static String extensionFromUrl(String url) {
        int dot = url.lastIndexOf('.');
        return dot >= 0 ? url.substring(dot) : ".png";
    }

    private static String relativeAssetPath(JekyllLayoutConvention convention, String uuid, String ext) {
        return convention.assetsRelative() + "/" + uuid + ext;
    }

    public static Pattern relativeAssetPattern(JekyllLayoutConvention convention) {
        String dir = Pattern.quote(convention.assetsRelative());
        return Pattern.compile("(?:\\.\\./)*" + dir + "/([0-9a-fA-F\\-]{36})(\\.[a-zA-Z0-9]+)");
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
            fm.put("cover", relativeAssetPath(convention, cover.getUuid(), extensionFromUrl(cover.getUrl())));
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
                    Path dest = assetsDir.resolve(uuid + ext);
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

    private void ensureImageInDb(Blog blog, String uuid, String ext, Path source) {
        if (!Files.exists(source)) {
            return;
        }
        if (imageRepository.findByUuid(uuid).isPresent()) {
            return;
        }
        try {
            byte[] content = Files.readAllBytes(source);
            imageService.storeImportedImage(blog, uuid, ext, content, contentTypeForExt(ext));
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

    @Transactional
    public void importAssetsFromWorkspace(Blog blog, Path workspace, JekyllLayoutConvention convention) throws IOException {
        Path assetsDir = convention.resolveAssets(workspace);
        if (!Files.isDirectory(assetsDir)) {
            return;
        }
        try (Stream<Path> files = Files.list(assetsDir)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                String name = file.getFileName().toString();
                int dot = name.lastIndexOf('.');
                if (dot <= 0) {
                    continue;
                }
                String uuid = name.substring(0, dot);
                String ext = name.substring(dot);
                ensureImageInDb(blog, uuid, ext, file);
            }
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
            matcher.appendReplacement(sb, Matcher.quoteReplacement(relativeAssetPath(convention, uuid, ext)));
        }
        matcher.appendTail(sb);
        return rewriteMarkdownAlts(sb.toString());
    }

    public String prepareBodyForImport(String body, Blog blog, Path workspace, JekyllLayoutConvention convention) throws IOException {
        if (body == null) {
            return "";
        }
        importAssetsFromWorkspace(blog, workspace, convention);
        Matcher m = relativeAssetPattern(convention).matcher(body);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String uuid = m.group(1);
            String ext = m.group(2);
            ensureImageInDb(blog, uuid, ext, convention.resolveAssets(workspace).resolve(uuid + ext));
            m.appendReplacement(sb, Matcher.quoteReplacement("/api/images/" + uuid + ext));
        }
        m.appendTail(sb);
        return markerService.toStoredContent(sb.toString());
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
            m.appendReplacement(sb, Matcher.quoteReplacement("![" + escapeMarkdownAlt(alt) + "](" + url + ")"));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
