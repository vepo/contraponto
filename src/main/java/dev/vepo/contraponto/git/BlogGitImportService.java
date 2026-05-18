package dev.vepo.contraponto.git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
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
import dev.vepo.contraponto.image.ImageRepository;
import dev.vepo.contraponto.image.PostImageDependencyService;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostPublicationService;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.renderer.Format;
import dev.vepo.contraponto.serie.SerieService;
import dev.vepo.contraponto.tag.TagService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

@ApplicationScoped
public class BlogGitImportService {

    /** Matches {@linkplain JekyllLayoutConvention#postsRelative()} vs drafts. */
    public enum SourceKind {
        POSTS_FOLDER,
        DRAFTS_FOLDER
    }

    private static final Logger LOG = LoggerFactory.getLogger(BlogGitImportService.class);

    private final PostRepository postRepository;
    private final PostPublicationService publicationService;
    private final TagService tagService;
    private final SerieService serieService;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;
    private final PostGitMarkdownCodec markdownCodec;

    private final GitImageSyncService gitImageSyncService;

    private final ImageRepository imageRepository;

    private final PostImageDependencyService postImageDependencyService;

    @Inject
    public BlogGitImportService(PostRepository postRepository,
                                PostPublicationService publicationService,
                                TagService tagService,
                                SerieService serieService,
                                EntityManager entityManager,
                                ObjectMapper objectMapper,
                                PostGitMarkdownCodec markdownCodec,
                                GitImageSyncService gitImageSyncService,
                                ImageRepository imageRepository,
                                PostImageDependencyService postImageDependencyService) {
        this.postRepository = postRepository;
        this.publicationService = publicationService;
        this.tagService = tagService;
        this.serieService = serieService;
        this.entityManager = entityManager;
        this.objectMapper = objectMapper;
        this.markdownCodec = markdownCodec;
        this.gitImageSyncService = gitImageSyncService;
        this.imageRepository = imageRepository;
        this.postImageDependencyService = postImageDependencyService;
    }

    @Transactional(value = TxType.REQUIRES_NEW)
    public GitSyncPostResult ingest(long blogId,
                                    Path workspace,
                                    JekyllLayoutConvention convention,
                                    Path markdownPath,
                                    SourceKind sourceKind) {
        String pathLabel = markdownPath.toString();
        try {
            Blog blog = entityManager.find(Blog.class, blogId);
            if (blog == null) {
                return GitSyncPostResult.skipped(pathLabel, "Blog was not found.", null);
            }
            Path fileNamePath = markdownPath.getFileName();
            if (fileNamePath == null) {
                return GitSyncPostResult.skipped(pathLabel, "Markdown file has no name.", null);
            }

            ParsedPathStem stem = ParsedPathStem.from(fileNamePath.toString(), sourceKind);
            if (stem.slug().isBlank()) {
                String remediation = sourceKind == SourceKind.POSTS_FOLDER
                                                                           ? "Rename published files to yyyy-MM-dd-slug.md under _posts/."
                                                                           : "Rename draft files to slug.md under _drafts/.";
                return GitSyncPostResult.skipped(pathLabel,
                                                 "Could not read a slug from the file name.",
                                                 remediation);
            }

            PostGitMarkdownCodec.ParsedFrontMatterMarkdown doc = readMarkdown(markdownPath);
            IngestedPostDraft draft = resolvePostDraft(doc, stem, sourceKind, blog);
            applyPostFields(draft, doc, blog, workspace, convention);
            finalizeIngestion(draft, doc);
            return GitSyncPostResult.success(draft.post().getId(), pathLabel,
                                             "Imported post \"" + draft.slug() + "\".");
        } catch (Exception ex) {
            LOG.warn("Import failed markdown={}: {}", markdownPath, ex.toString());
            return GitSyncPostResult.failed(pathLabel,
                                            "Could not import this post from Git.",
                                            "Check YAML front matter, slug, and contraponto_post_id. See the layout convention.",
                                            ex.toString());
        }
    }

    private PostGitMarkdownCodec.ParsedFrontMatterMarkdown readMarkdown(Path markdownPath) throws IOException {
        return markdownCodec.parseMarkdownDocument(Files.readString(markdownPath));
    }

    private IngestedPostDraft resolvePostDraft(PostGitMarkdownCodec.ParsedFrontMatterMarkdown doc,
                                               ParsedPathStem stem,
                                               SourceKind sourceKind,
                                               Blog blog) {
        String slugYaml = trimToNull(doc.frontMatter().get("slug"));
        String slug = slugYaml != null ? slugYaml.toLowerCase(Locale.ROOT) : stem.slug().toLowerCase(Locale.ROOT);

        String title = trimToNull(doc.frontMatter().get("title"));
        if (title == null || title.isBlank()) {
            title = stem.slug().replace('-', ' ');
        }

        boolean published = parseBoolean(doc.frontMatter().get("published")).orElse(sourceKind == SourceKind.POSTS_FOLDER);
        Optional<Post> existing = locateExisting(doc, slug, blog);
        Post post = existing.orElseGet(() -> wireNewDraftPostStub(blog));
        return new IngestedPostDraft(post, existing.isPresent(), slug, title, published, stem);
    }

    private void applyPostFields(IngestedPostDraft draft,
                                 PostGitMarkdownCodec.ParsedFrontMatterMarkdown doc,
                                 Blog blog,
                                 Path workspace,
                                 JekyllLayoutConvention convention)
            throws IOException {
        Post post = draft.post();
        String description = Objects.requireNonNullElse(trimToNull(doc.frontMatter().get("description")), "");
        String body = Objects.requireNonNullElse(doc.body(), "");
        body = gitImageSyncService.prepareBodyForImport(body.stripTrailing(), blog, workspace, convention);

        serieService.applySerieTitleToPost(post, trimToNull(doc.frontMatter().get("serie")));

        post.setSlug(draft.slug());
        post.setTitle(draft.title());
        post.setDescription(description);
        post.setContent(body);
        applyCoverFromFrontMatter(post, trimToNull(doc.frontMatter().get("cover")), convention, workspace, blog);
        post.setFormat(parseFormat(trimToNull(doc.frontMatter().get("format"))));

        Optional<Boolean> featuredFlag = parseBoolean(doc.frontMatter().get("featured"));
        if (featuredFlag.isPresent()) {
            post.setFeatured(featuredFlag.get());
        } else if (!draft.existedBefore()) {
            post.setFeatured(false);
        }

        LocalDateTime now = LocalDateTime.now();
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
        LocalDateTime inferred = draft.stem().optionalPublishedDay().map(LocalDate::atStartOfDay).orElse(null);
        LocalDateTime fmTime = parseDateTime(trimToNull(doc.frontMatter().get("published_at")));
        LocalDateTime effective = fmTime != null ? fmTime : Objects.requireNonNullElse(inferred, now);
        if (!draft.existedBefore() || fmTime != null || inferred != null) {
            post.setPublishedAt(effective);
        } else if (post.getPublishedAt() == null) {
            post.setPublishedAt(now);
        }
    }

    private void finalizeIngestion(IngestedPostDraft draft, PostGitMarkdownCodec.ParsedFrontMatterMarkdown doc)
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
        entityManager.flush();
    }

    private record IngestedPostDraft(Post post,
                                     boolean existedBefore,
                                     String slug,
                                     String title,
                                     boolean published,
                                     ParsedPathStem stem) {}

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
        String uuid = m.group(1);
        try {
            gitImageSyncService.importAssetsFromWorkspace(blog, workspace, convention);
        } catch (IOException e) {
            LOG.warn("Could not import assets for cover: {}", coverPath, e);
        }
        imageRepository.findByUuid(uuid).ifPresent(post::setCover);
    }

    private Post wireNewDraftPostStub(Blog blog) {
        Post post = new Post();
        post.setBlog(blog);
        return post;
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

    private void attachTags(Object rawYamlTags, Post post) throws IOException {
        List<String> tags = readYamlTags(rawYamlTags);
        String json = tags.isEmpty() ? "[]" : objectMapper.writeValueAsString(tags);
        tagService.syncPostTags(post, json);
    }

    private static Format parseFormat(String fm) {
        if (fm == null || fm.isBlank()) {
            return Format.MARKDOWN;
        }
        try {
            return Format.valueOf(fm.strip().replace(' ', '_').toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException _) {
            return Format.MARKDOWN;
        }
    }

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

    private static LocalDateTime parseDateTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(raw).toLocalDateTime();
        } catch (DateTimeParseException _) {
            try {
                return LocalDateTime.parse(raw);
            } catch (DateTimeParseException _) {
                try {
                    LocalDate dateOnly = LocalDate.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE);
                    return dateOnly.atStartOfDay();
                } catch (DateTimeParseException _) {
                    return null;
                }
            }
        }
    }

    private static String trimToNull(Object o) {
        if (o == null) {
            return null;
        }
        String s = o.toString().strip();
        return s.isEmpty() ? null : s;
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
}
