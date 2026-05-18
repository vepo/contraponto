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
    public void ingest(long blogId,
                       Path workspace,
                       JekyllLayoutConvention convention,
                       Path markdownPath,
                       SourceKind sourceKind)
            throws IOException {
        Blog blog = entityManager.find(Blog.class, blogId);
        if (blog == null) {
            return;
        }
        Path fileNamePath = markdownPath.getFileName();
        if (fileNamePath == null) {
            return;
        }

        ParsedPathStem stem = ParsedPathStem.from(fileNamePath.toString(), sourceKind);
        if (stem.slug().isBlank()) {
            LOG.warn("Skip markdown without usable slug (filename invalid): {}", markdownPath);
            return;
        }

        String raw = Files.readString(markdownPath);
        PostGitMarkdownCodec.ParsedFrontMatterMarkdown doc = markdownCodec.parseMarkdownDocument(raw);

        String slugYaml = trimToNull(doc.frontMatter().get("slug"));
        String slug = slugYaml != null ? slugYaml.toLowerCase(Locale.ROOT) : stem.slug().toLowerCase(Locale.ROOT);

        String title = trimToNull(doc.frontMatter().get("title"));
        if (title == null || title.isBlank()) {
            title = stem.slug().replace('-', ' ');
        }

        Optional<Boolean> yamlPublishedFlag = parseBoolean(doc.frontMatter().get("published"));
        boolean folderDefaultPublished = switch (sourceKind) {
            case POSTS_FOLDER -> true;
            case DRAFTS_FOLDER -> false;
        };
        boolean published = yamlPublishedFlag.orElse(folderDefaultPublished);

        Optional<Post> existing = locateExisting(doc, slug, blog);
        Post post = existing.orElseGet(() -> wireNewDraftPostStub(blog));
        boolean existedBefore = existing.isPresent();

        String description = Objects.requireNonNullElse(trimToNull(doc.frontMatter().get("description")), "");
        String body = Objects.requireNonNullElse(doc.body(), "");
        body = gitImageSyncService.prepareBodyForImport(body.stripTrailing(), blog, workspace, convention);

        serieService.applySerieTitleToPost(post, trimToNull(doc.frontMatter().get("serie")));

        post.setSlug(slug);
        post.setTitle(title);
        post.setDescription(description);
        post.setContent(body);
        applyCoverFromFrontMatter(post, trimToNull(doc.frontMatter().get("cover")), convention, workspace, blog);
        post.setFormat(parseFormat(trimToNull(doc.frontMatter().get("format"))));

        Optional<Boolean> featuredFlag = parseBoolean(doc.frontMatter().get("featured"));
        if (featuredFlag.isPresent()) {
            post.setFeatured(featuredFlag.get());
        } else if (!existedBefore) {
            post.setFeatured(false);
        }

        LocalDateTime now = LocalDateTime.now();
        if (!existedBefore) {
            post.setCreatedAt(now);
        }

        post.setPublished(published);
        if (published) {
            LocalDateTime inferred = stem.optionalPublishedDay().map(LocalDate::atStartOfDay).orElse(null);
            LocalDateTime fmTime = parseDateTime(trimToNull(doc.frontMatter().get("published_at")));
            LocalDateTime effective = fmTime != null ? fmTime : Objects.requireNonNullElse(inferred, now);
            if (!existedBefore || fmTime != null || inferred != null) {
                post.setPublishedAt(effective);
            } else if (post.getPublishedAt() == null) {
                post.setPublishedAt(now);
            }
        } else {
            post.setPublishedAt(null);
        }

        if (!existedBefore) {
            postRepository.saveFromGit(post);
        }
        attachTags(doc.frontMatter().get("tags"), post);
        postImageDependencyService.syncPostDependencies(post);
        if (published) {
            publicationService.publish(post);
        }
        entityManager.flush();
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
