package dev.vepo.contraponto.git;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.git.BlogGitImportService.SourceKind;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.renderer.Format;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class BlogGitImportServiceTest {

    private static User baselineUser() {
        int token = ThreadLocalRandom.current().nextInt(100_000_000);
        return Given.user()
                    .withUsername("u_%08x".formatted(Integer.toUnsignedLong(token)))
                    .withEmail("e_%x@test".formatted(token))
                    .withName("Importer")
                    .withPassword("Password123!")
                    .persist();
    }

    private static Path ingestDir() throws Exception {
        return Files.createTempDirectory("git-import");
    }

    @Inject
    BlogGitImportService blogGitImportService;

    @Inject
    PostRepository postRepository;

    private long authoredPostCount(long authorUserId) {
        return postRepository.countByAuthorAndPublished(authorUserId, false)
                + postRepository.countByAuthorAndPublished(authorUserId, true);
    }

    @Test
    void ingestCreatesPublishedPostFromPostsFolderWithCommaTagsAndFormatFallback() throws Exception {
        Blog blog = baselineUser().getDefaultBlog();
        Path dated = ingestDir().resolve("2026-05-01-imported-slug.md");
        Files.writeString(dated,
                          """
                          ---
                          title: Imported Title
                          slug: Imported-Slug
                          description: Intro
                          format: UNKNOWN_FORMAT
                          published: yes
                          featured: off
                          published_at: 2026-05-05T08:09:10+03:00
                          tags: left, RIGHT
                          ---

                             trailing whitespace line

                             """,
                          StandardCharsets.UTF_8);

        blogGitImportService.ingest(blog.getId(), dated, SourceKind.POSTS_FOLDER);

        Optional<Post> found = postRepository.findByBlogIdAndSlugWithTags(blog.getId(), "imported-slug");
        assertThat(found).isPresent();
        Post p = found.get();
        assertThat(p.getTitle()).contains("Imported");
        assertThat(p.getDescription()).isEqualTo("Intro");
        assertThat(p.getFormat()).isEqualTo(Format.MARKDOWN);
        assertThat(p.isFeatured()).isFalse();
        assertThat(p.isPublished()).isTrue();
        assertThat(p.getPublishedAt()).isNotNull();
        assertThat(p.getContent().stripTrailing()).contains("trailing whitespace");
    }

    @Test
    void ingestDoesNothingWhenBlogDoesNotExist() throws Exception {
        Path md = Files.createTempFile("missing-blog-", ".md");
        Files.writeString(md,
                          """
                          ---
                          title: X
                          ---

                          B""",
                          StandardCharsets.UTF_8);

        long before = postRepository.count();
        blogGitImportService.ingest(Long.MAX_VALUE, md, SourceKind.POSTS_FOLDER);
        assertThat(postRepository.count()).isEqualTo(before);
        assertThat(Files.readString(md)).contains("title: X");
    }

    @Test
    void ingestSkipsPostsFolderMarkdownWithUnusableFilename() throws Exception {
        User user = baselineUser();
        Blog blog = user.getDefaultBlog();
        Path md = Files.createTempFile("illegal-name-", ".md");
        Files.writeString(md,
                          """
                          ---
                          title: Trap
                          ---

                          z""",
                          StandardCharsets.UTF_8);

        long beforePosts = authoredPostCount(user.getId());
        blogGitImportService.ingest(blog.getId(), md, SourceKind.POSTS_FOLDER);
        assertThat(authoredPostCount(user.getId())).isEqualTo(beforePosts);
        assertThat(Files.readString(md)).contains("Trap");
    }

    @Test
    void ingestStoresDraftSlugFromFilenameWithoutDefaultPublishedTimestamp() throws Exception {
        Blog blog = baselineUser().getDefaultBlog();
        Path md = ingestDir().resolve("short-draft-note.md");
        Files.writeString(md,
                          """
                          ---
                          title: Only Draft
                          published: false
                          tags:
                            - zig
                          ---

                          d""",
                          StandardCharsets.UTF_8);

        blogGitImportService.ingest(blog.getId(), md, SourceKind.DRAFTS_FOLDER);

        Optional<Post> fetched = postRepository.findByBlogIdAndSlugWithTags(blog.getId(), "short-draft-note");
        assertThat(fetched).isPresent();
        Post p = fetched.get();
        assertThat(p.isPublished()).isFalse();
        assertThat(p.getPublishedAt()).isNull();
        assertThat(p.getTitle()).isEqualTo("Only Draft");
    }

    @Test
    void ingestUpdatesExistingPostWhenContrapontoIdMatchesBlog() throws Exception {
        User user = baselineUser();
        Blog blog = user.getDefaultBlog();
        Post existing = Given.post()
                             .withAuthor(user)
                             .withBlog(blog)
                             .withTitle("Old")
                             .withSlug("stable-slug")
                             .withContent("legacy body")
                             .withPublished(false)
                             .persist();
        long id = existing.getId();

        Path dated = ingestDir().resolve("2026-05-02-stable-slug.md");
        Files.writeString(dated,
                          """
                          ---
                          contraponto_post_id: %d
                          title: Renovated Title
                          format: asciidoc
                          featured: TRUE
                          published: true
                          published_at: 2026-04-02
                          ---
                          Renewal body""".formatted(id),
                          StandardCharsets.UTF_8);

        blogGitImportService.ingest(blog.getId(), dated, SourceKind.POSTS_FOLDER);

        Optional<Post> fetched = postRepository.findByBlogIdAndSlugWithTags(blog.getId(), "stable-slug");
        assertThat(fetched).isPresent();
        Post p = fetched.get();
        assertThat(p.getId()).isEqualTo(id);
        assertThat(p.getTitle()).isEqualTo("Renovated Title");
        assertThat(p.getFormat()).isEqualTo(Format.ASCIIDOC);
        assertThat(p.isFeatured()).isTrue();
        assertThat(p.getContent()).contains("Renewal");
        assertThat(p.isPublished()).isTrue();
    }

    @BeforeEach
    void wipeData() {
        Given.cleanup();
    }
}
