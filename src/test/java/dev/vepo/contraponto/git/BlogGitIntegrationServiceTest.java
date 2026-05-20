package dev.vepo.contraponto.git;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.StoredConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@QuarkusIntegrationTest
class BlogGitIntegrationServiceTest {

    private static void addUpstreamMarkdown(Path upstream, String relativeRepoPath, String body) throws Exception {
        try (Git git = Git.open(upstream.toFile())) {
            Path dest = upstream.resolve(relativeRepoPath);
            Files.createDirectories(dest.getParent());
            Files.writeString(dest, body, StandardCharsets.UTF_8);
            String pattern = relativeRepoPath.replace('\\', '/');
            git.add().addFilepattern(pattern).call();
            git.commit().setMessage("extra").call();
        }
    }

    private static Long blogId(User user) {
        return user.getDefaultBlog().getId();
    }

    /** Returns the tracked default branch produced by {@code git init}. */
    private static String bootstrapUpstreamRepo(Path upstream) throws Exception {
        try (Git git = Git.init().setDirectory(upstream.toFile()).call()) {
            StoredConfig cfg = git.getRepository().getConfig();
            cfg.setString("user", null, "name", "fixture");
            cfg.setString("user", null, "email", "fixture@fixture");
            cfg.save();

            Files.createDirectories(upstream.resolve("custom/posts"));
            Files.createDirectories(upstream.resolve("custom/_drafts"));
            Files.writeString(upstream.resolve("_contraponto.yml"),
                              """
                              posts_directory: custom/posts


                              drafts_directory: custom/_drafts


                              layout_fm_key: layout


                              default_layout: post""");

            Files.writeString(upstream.resolve("custom/posts/2026-12-01-mirror-post.md"),
                              """
                              ---
                              title: Remote Title
                              ---
                              Remote body """);

            git.add().addFilepattern(".").call();
            git.commit().setMessage("seed").call();
            return git.getRepository().getBranch();
        }
    }

    private static void deleteRecursively(Path root) throws Exception {
        if (!Files.exists(root)) {
            return;
        }
        try (java.util.stream.Stream<Path> stream = Files.walk(root)) {
            stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (Exception _) {
                    // Best-effort cleanup during test teardown
                }
            });
        }
    }

    private static Blog reloadBlog(Long blogId) {
        return Given.transaction(() -> Given.inject(EntityManager.class).find(Blog.class, blogId));
    }

    @Inject
    BlogGitIntegrationTransaction integrationTransaction;

    @Inject
    PostRepository postRepository;

    @Inject
    ContrapontoGitSettings contrapontoGitSettings;

    private User persistUserLinkedToUpstream(Path upstream, String branch) {
        int token = java.util.concurrent.ThreadLocalRandom.current().nextInt();
        User u =
                Given.user()
                     .withUsername("g_%06x".formatted(Integer.toUnsignedLong(token)))
                     .withEmail("m_%08x@t.st".formatted(Integer.toUnsignedLong(token)))
                     .withName("Git Tester")
                     .withPassword("Password123!")
                     .persist();

        String remoteUri = upstream.toAbsolutePath().toUri().toString();

        Given.transaction(() -> {
            EntityManager em = Given.inject(EntityManager.class);
            Blog managed = em.find(Blog.class, u.getDefaultBlog().getId());
            managed.setGitEnabled(true);
            managed.setGitRemoteUrl(remoteUri);
            managed.setGitBranch(branch);
            em.merge(managed);
        });

        Blog verified = reloadBlog(u.getDefaultBlog().getId());
        assertThat(verified.isGitEnabled()).isTrue();
        assertThat(verified.getGitRemoteUrl()).isEqualTo(remoteUri);

        return Given.inject(EntityManager.class).find(User.class, u.getId());
    }

    @BeforeEach
    void resetWorkspaceFilesystem() throws Exception {
        Given.cleanup();
        Path root = resolvedGitWorkspaceRoot();
        if (Files.exists(root)) {
            deleteRecursively(root);
        }
        Files.deleteIfExists(root);
    }

    private Path resolvedGitWorkspaceRoot() {
        return contrapontoGitSettings.workspaceRoot()
                                     .filter(s -> !s.strip().isEmpty())
                                     .map(s -> Path.of(s.strip()).toAbsolutePath())
                                     .orElseGet(() -> Path.of(System.getProperty("java.io.tmpdir")).resolve("contraponto-git")
                                                          .toAbsolutePath());
    }

    @Test
    void syncImportsCustomLayoutCommitsSecondPullAndExportsPosts() throws Exception {
        Path upstream = Files.createTempDirectory("git-upstream-");
        String branch = bootstrapUpstreamRepo(upstream);

        User user = persistUserLinkedToUpstream(upstream, branch);

        integrationTransaction.syncBlogFromGit(blogId(user));

        Path workspace = workspaceDir(blogId(user));
        assertThat(workspace.resolve(".git")).exists();

        Optional<Post> mirror = postRepository.findByBlogIdAndSlugWithTags(blogId(user), "mirror-post");
        assertThat(mirror).isPresent();
        Post mp = mirror.get();
        assertThat(mp.getTitle()).isEqualTo("Remote Title");
        assertThat(mp.getContent()).contains("Remote body");
        assertThat(mp.isPublished()).isTrue();
        assertThat(mp.getPublishedAt()).isNotNull();
        assertThat(mp.getPublishedAt().toLocalDate()).isEqualTo(LocalDate.of(2026, 12, 1));

        addUpstreamMarkdown(upstream, "custom/posts/2027-02-03-secondary.md",
                            """
                            ---
                            title: Second Wave
                            ---
                            Second wave body""");

        integrationTransaction.syncBlogFromGit(blogId(user));

        Optional<Post> second = postRepository.findByBlogIdAndSlugWithTags(blogId(user), "secondary");
        assertThat(second).isPresent();
        assertThat(second.get().getTitle()).isEqualTo("Second Wave");

        Given.post().withAuthor(user).withSlug("exported-one").withTitle("Export Me").withContent("draft").withPublished(false).persist();

        Optional<Post> draft = postRepository.findByBlogIdAndSlugWithTags(blogId(user), "exported-one");
        assertThat(draft).isPresent();
        integrationTransaction.exportPost(draft.get().getId());

        Path draftFile = workspace.resolve("custom/_drafts/exported-one.md");
        assertThat(draftFile).exists();
        assertThat(Files.readString(draftFile, StandardCharsets.UTF_8)).contains("Export Me");

        Given.post().withAuthor(user).withSlug("pub-post").withTitle("Pub Title").withContent("pub").withPublished(true).persist();

        Optional<Post> pub = postRepository.findByBlogIdAndSlugWithTags(blogId(user), "pub-post");
        assertThat(pub).isPresent();
        integrationTransaction.exportPost(pub.get().getId());

        LocalDate exportDay =
                Optional.ofNullable(pub.get().getPublishedAt()).map(java.time.LocalDateTime::toLocalDate).orElse(LocalDate.now());
        Path publishedMd = workspace.resolve("custom/posts/" + exportDay + "-pub-post.md");
        assertThat(publishedMd).exists();
        String written = Files.readString(publishedMd, StandardCharsets.UTF_8);
        assertThat(written).contains("Pub Title")
                           .contains("contraponto_post_id:");

        Blog managed = reloadBlog(blogId(user));
        assertThat(managed.getGitLastKnownCommit()).isNotNull();
        assertThat(managed.isGitEnabled()).isTrue();
    }

    @Test
    void syncSkipsImportWhenRemoteHeadMatchesWatermark() throws Exception {
        Path upstream = Files.createTempDirectory("git-watermark-");
        String branch = bootstrapUpstreamRepo(upstream);
        User user = persistUserLinkedToUpstream(upstream, branch);

        integrationTransaction.syncBlogFromGit(blogId(user));
        assertThat(postRepository.findByBlogIdAndSlugWithTags(blogId(user), "mirror-post")).isPresent();
        Blog afterFirst = reloadBlog(blogId(user));
        assertThat(afterFirst.getGitLastKnownCommit()).isNotNull();

        integrationTransaction.syncBlogFromGit(blogId(user));

        assertThat(postRepository.findByBlogIdAndSlugWithTags(blogId(user), "mirror-post")).isPresent();
        assertThat(reloadBlog(blogId(user)).getGitLastKnownCommit()).isEqualTo(afterFirst.getGitLastKnownCommit());
    }

    private Path workspaceDir(long blogId) {
        Path root = resolvedGitWorkspaceRoot().resolve("blog-" + blogId);
        assertThat(root).exists();
        return root;
    }
}