package dev.vepo.contraponto.git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.image.ImageRepository;
import dev.vepo.contraponto.image.PostImageDependencyService;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostPublicationDescriptions;
import dev.vepo.contraponto.post.PostPublicationService;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.renderer.Format;
import dev.vepo.contraponto.serie.SerieService;
import dev.vepo.contraponto.tag.TagService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

@ApplicationScoped
public class BlogGitImportService {

    private record IngestedPostDraft(Post post,
                                     boolean existedBefore,
                                     String slug,
                                     String title,
                                     boolean published,
                                     ParsedPathStem stem) {}

    private record ParsedPathStem(String slug, Optional<LocalDate> optionalPublishedDay) {
        static ParsedPathStem from(String fileName, SourceKind kind) {
            if (kind == SourceKind.DRAFTS_FOLDER) {
                return new ParsedPathStem(stripExt(fileName), Optional.empty());
            }
            var m = PostGitMarkdownCodec.PUBLISHED_POST_FILENAME.matcher(fileName);
            if (!m.matches()) {
                return new ParsedPathStem("", Optional.empty());
            }
            String slugStem = m.group("slug");
            String dateStem = m.group("date");
            try {
                LocalDate d = LocalDate.parse(dateStem, DateTimeFormatter.ISO_LOCAL_DATE);
                return new ParsedPathStem(slugStem, Optional.of(d));
            } catch (DateTimeParseException _) {
                return new ParsedPathStem(slugStem, Optional.empty());
            }
        }

        static String stripExt(String name) {
            int dot = name.lastIndexOf('.');
            return dot > 0 ? name.substring(0, dot) : name;
        }
    }

    /** Matches {@linkplain JekyllLayoutConvention#postsRelative()} vs drafts. */
    public enum SourceKind {
        POSTS_FOLDER,
        DRAFTS_FOLDER
    }

    private static final Logger LOG = LoggerFactory.getLogger(BlogGitImportService.class);

    private static Optional<Boolean> parseBoolean(Object raw) {
        if (raw == null) {
            return Optional.empty();
        }
        if (raw instanceof Boolean b) {
            return Optional.of(b);
        }
        String s = raw.toString().strip().toLowerCase(Locale.ROOT);
        return switch (s) {
            case "true", "yes", "on" -> Optional.of(true);
            case "false", "no", "off" -> Optional.of(false);
            default -> Optional.empty();
        };
    }

    private static Long parseLong(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number n) {
            return n.longValue();
        }
        try {
            String s = raw.toString().strip();
            if (s.isEmpty()) {
                return null;
            }
            return Long.parseLong(s);
        } catch (NumberFormatException _) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> readYamlTags(Object rawYamlTags) {
        if (rawYamlTags == null) {
            return List.of();
        }
        if (rawYamlTags instanceof List<?> lst) {
            List<String> out = new ArrayList<>();
            for (Object o : lst) {
                if (o != null && !o.toString().isBlank()) {
                    out.add(o.toString().strip());
                }
            }
            return out;
        }
        if (rawYamlTags instanceof String s) {
            if (s.isBlank()) {
                return List.of();
            }
            List<String> out = new ArrayList<>();
            for (String p : s.split(",")) {
                String t = p.strip();
                if (!t.isEmpty()) {
                    out.add(t);
                }
            }
            return out;
        }
        return List.of();
    }

    private static String trimToNull(Object o) {
        if (o == null) {
            return null;
        }
        String s = o.toString().strip();
        return s.isEmpty() ? null : s;
    }

    private final PostRepository postRepository;

    private final PostPublicationService publicationService;

    private final TagService tagService;

    private final SerieService serieService;

    private final BlogRepository blogRepository;

    private final ObjectMapper objectMapper;

    private final PostGitMarkdownCodec markdownCodec;

    private final GitImageSyncService gitImageSyncService;

    private final ImageRepository imageRepository;

    private final PostImageDependencyService postImageDependencyService;

    private final GitSyncRunService gitSyncRunService;

    private final GitImportFailureMapper gitImportFailureMapper;

    @Inject
    public BlogGitImportService(PostRepository postRepository,
                                PostPublicationService publicationService,
                                TagService tagService,
                                SerieService serieService,
                                BlogRepository blogRepository,
                                ObjectMapper objectMapper,
                                PostGitMarkdownCodec markdownCodec,
                                GitImageSyncService gitImageSyncService,
                                ImageRepository imageRepository,
                                PostImageDependencyService postImageDependencyService,
                                GitSyncRunService gitSyncRunService,
                                GitImportFailureMapper gitImportFailureMapper) {
        this.postRepository = postRepository;
        this.publicationService = publicationService;
        this.tagService = tagService;
        this.serieService = serieService;
        this.blogRepository = blogRepository;
        this.objectMapper = objectMapper;
        this.markdownCodec = markdownCodec;
        this.gitImageSyncService = gitImageSyncService;
        this.imageRepository = imageRepository;
        this.postImageDependencyService = postImageDependencyService;
        this.gitSyncRunService = gitSyncRunService;
        this.gitImportFailureMapper = gitImportFailureMapper;
    }

    private void applyCoverFromFrontMatter(Post post,
                                           String coverPath,
                                           JekyllLayoutConvention convention,
                                           Path workspace,
                                           Blog blog) {
        if (coverPath == null || coverPath.isBlank()) {
            return;
        }
        Matcher m = GitImageSyncService.relativeAssetPattern(convention).matcher(coverPath);
        if (!m.find()) {
            return;
        }
        String assetRel = m.group(1);
        String ext = m.group(2);
        String basename = GitFrontMatterResolver.assetBasename(assetRel);
        String uuid = GitImportedAssetId.normalize(basename, ext);
        gitImageSyncService.importAssetFromRelativePath(blog, workspace, convention, assetRel, ext);
        imageRepository.findByUuid(uuid).ifPresent(post::setCover);
    }

    private void applyPostFields(IngestedPostDraft draft,
                                 PostGitMarkdownCodec.ParsedFrontMatterMarkdown doc,
                                 Blog blog,
                                 Path workspace,
                                 JekyllLayoutConvention convention,
                                 Path postFile)
            throws IOException {
        Post post = draft.post();
        String description = Objects.requireNonNullElse(trimToNull(doc.frontMatter().get("description")), "");
        String rawBody = Objects.requireNonNullElse(doc.body(), "");
        String coverPath = GitFrontMatterResolver.resolveCoverPath(doc.frontMatter());
        Format format = GitFrontMatterResolver.resolveFormat(doc.frontMatter(), postFile);
        gitImageSyncService.importImagesForPost(blog, workspace, convention, coverPath, rawBody, format);
        String body = gitImageSyncService.prepareBodyForImport(rawBody.stripTrailing(), blog, workspace, convention, format);

        serieService.applySerieTitleToPost(post, GitFrontMatterResolver.resolveSerieTitle(doc.frontMatter()));

        post.setSlug(draft.slug());
        post.setTitle(draft.title());
        post.setDescription(PostPublicationDescriptions.truncateForPublication(description));
        post.setContent(body);
        applyCoverFromFrontMatter(post, coverPath, convention, workspace, blog);
        post.setFormat(format);

        Optional<Boolean> featuredFlag = parseBoolean(doc.frontMatter().get("featured"));
        if (featuredFlag.isPresent()) {
            post.setFeatured(featuredFlag.get());
        } else if (!draft.existedBefore()) {
            post.setFeatured(false);
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        if (!draft.existedBefore()) {
            post.setCreatedAt(now);
        }
        applyPublishedState(post, draft, doc, now);
    }

    private void applyPublishedState(Post post,
                                     IngestedPostDraft draft,
                                     PostGitMarkdownCodec.ParsedFrontMatterMarkdown doc,
                                     LocalDateTime now) {
        post.setPublished(draft.published());
        if (!draft.published()) {
            post.setPublishedAt(null);
            return;
        }
        LocalDateTime fmTime = GitFrontMatterResolver.resolvePublishedAt(doc.frontMatter(), draft.stem().optionalPublishedDay());
        LocalDateTime effective = fmTime != null ? fmTime : now;
        if (!draft.existedBefore() || fmTime != null) {
            post.setPublishedAt(effective);
        } else if (post.getPublishedAt() == null) {
            post.setPublishedAt(now);
        }
    }

    private void attachTags(Object rawYamlTags, Post post) throws IOException {
        List<String> tags = readYamlTags(rawYamlTags);
        String json = tags.isEmpty() ? "[]" : objectMapper.writeValueAsString(tags);
        tagService.syncPostTags(post, json);
    }

    private void finalizeIngestion(IngestedPostDraft draft,
                                   PostGitMarkdownCodec.ParsedFrontMatterMarkdown doc,
                                   String markdownPath)
            throws IOException {
        Post post = draft.post();
        if (!draft.existedBefore()) {
            postRepository.saveFromGit(post);
        }
        attachTags(doc.frontMatter().get("tags"), post);
        postImageDependencyService.syncPostDependencies(post);
        if (draft.published()) {
            publicationService.publish(post);
        }
        postRepository.flush();
        String rawDescription = Objects.requireNonNullElse(trimToNull(doc.frontMatter().get("description")), "");
        if (draft.published() && PostPublicationDescriptions.exceedsPublicationLimit(rawDescription)) {
            gitSyncRunService.appendEntryCurrent(GitSyncRunEntryDraft.warnPost(
                                                                               GitSyncPhase.POST_IMPORT,
                                                                               post.getId(),
                                                                               markdownPath,
                                                                               "Description was longer than %s characters; the excerpt was truncated for storage and publishing."
                                                                                                                                                                                 .formatted(PostPublicationDescriptions.MAX_LENGTH),
                                                                               "Shorten the excerpt in Git or in Contraponto to fit the %s-character limit."
                                                                                                                                                            .formatted(PostPublicationDescriptions.MAX_LENGTH)));
        }
    }

    @Transactional(value = TxType.REQUIRES_NEW)
    public GitSyncPostResult ingest(long blogId,
                                    Path workspace,
                                    JekyllLayoutConvention convention,
                                    Path markdownPath,
                                    SourceKind sourceKind) {
        String pathLabel = markdownPath.toString();
        try {
            Blog blog = blogRepository.findById(blogId).orElse(null);
            if (blog == null) {
                return GitSyncPostResult.skipped(pathLabel, "Blog was not found.", null);
            }
            Path fileNamePath = markdownPath.getFileName();
            if (fileNamePath == null) {
                return GitSyncPostResult.skipped(pathLabel, "Markdown file has no name.", null);
            }

            ParsedPathStem stem = ParsedPathStem.from(fileNamePath.toString(), sourceKind);
            PostGitMarkdownCodec.ParsedFrontMatterMarkdown doc = readMarkdown(markdownPath);
            String slug = GitFrontMatterResolver.resolveSlug(doc.frontMatter(), stem.slug());
            if (slug.isBlank()) {
                String remediation = sourceKind == SourceKind.POSTS_FOLDER
                                                                           ? "Add slug or permalink in front matter, or rename to yyyy-MM-dd-slug.md under _posts/."
                                                                           : "Add slug or permalink in front matter, or rename to slug.md under _drafts/.";
                return GitSyncPostResult.skipped(pathLabel,
                                                 "Could not read a slug from front matter or the file name.",
                                                 remediation);
            }

            IngestedPostDraft draft = resolvePostDraft(doc, stem, sourceKind, blog, slug);
            applyPostFields(draft, doc, blog, workspace, convention, markdownPath);
            finalizeIngestion(draft, doc, pathLabel);
            return GitSyncPostResult.success(draft.post().getId(), pathLabel,
                                             "Imported post \"%s\".".formatted(draft.slug()));
        } catch (Exception ex) {
            LOG.warn("Import failed markdown={}: {}", markdownPath, ex.toString());
            GitImportFailureMapper.ClassifiedImportFailure classified = gitImportFailureMapper.classify(ex);
            return GitSyncPostResult.failed(pathLabel,
                                            classified.message(),
                                            classified.remediation(),
                                            ex.toString());
        }
    }

    private Optional<Post> locateExisting(PostGitMarkdownCodec.ParsedFrontMatterMarkdown doc, String slug, Blog blog) {
        Long hintedId = parseLong(doc.frontMatter().get(JekyllLayoutConvention.FM_POST_ID));
        if (hintedId != null) {
            Optional<Post> byId = postRepository.findByIdWithTags(hintedId);
            if (byId.filter(p -> p.getBlog().getId().equals(blog.getId())).isPresent()) {
                return byId;
            }
        }
        return postRepository.findByBlogIdAndSlugWithTags(blog.getId(), slug);
    }

    private PostGitMarkdownCodec.ParsedFrontMatterMarkdown readMarkdown(Path markdownPath) throws IOException {
        return markdownCodec.parseMarkdownDocument(Files.readString(markdownPath));
    }

    private IngestedPostDraft resolvePostDraft(PostGitMarkdownCodec.ParsedFrontMatterMarkdown doc,
                                               ParsedPathStem stem,
                                               SourceKind sourceKind,
                                               Blog blog,
                                               String slug) {
        String title = trimToNull(doc.frontMatter().get("title"));
        if (title == null || title.isBlank()) {
            String titleStem = stem.slug().isBlank() ? slug : stem.slug();
            title = titleStem.replace('-', ' ');
        }

        boolean folderDefault = sourceKind == SourceKind.POSTS_FOLDER;
        boolean published = GitFrontMatterResolver.resolvePublished(doc.frontMatter(), folderDefault);
        Optional<Post> existing = locateExisting(doc, slug, blog);
        Post post = existing.orElseGet(() -> wireNewDraftPostStub(blog));
        return new IngestedPostDraft(post, existing.isPresent(), slug, title, published, stem);
    }

    private Post wireNewDraftPostStub(Blog blog) {
        Post post = new Post();
        post.setBlog(blog);
        return post;
    }
}
