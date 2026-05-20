package dev.vepo.contraponto.git;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
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
import dev.vepo.contraponto.shared.pagination.PageQuery;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

/**
 * Exercises {@link BlogGitIntegrationTransaction#runScheduledImport} without an
 * outer test {@code @Transactional}, matching the async Git sync thread (no
 * request transaction).
 */
@QuarkusTest
class BlogGitIntegrationTransactionAsyncTest {

    private static Long blogId(User user) {
        return user.getDefaultBlog().getId();
    }

    private static String bootstrapUpstreamRepo(Path upstream) throws Exception {
        try (Git git = Git.init().setDirectory(upstream.toFile()).call()) {
            StoredConfig cfg = git.getRepository().getConfig();
            cfg.setString("user", null, "name", "fixture");
            cfg.setString("user", null, "email", "fixture@fixture");
            cfg.save();

            Files.createDirectories(upstream.resolve("custom/posts"));
            Files.writeString(upstream.resolve("_contraponto.yml"),
                              """
                              posts_directory: custom/posts


                              drafts_directory: custom/_drafts""");

            Files.writeString(upstream.resolve("custom/posts/2026-12-01-async-mirror.md"),
                              """
                              ---
                              title: Async Mirror
                              ---
                              Async body""");

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

    @Inject
    BlogGitIntegrationTransaction integrationTransaction;

    @Inject
    GitSyncRunService gitSyncRunService;

    @Inject
    PostRepository postRepository;

    @Inject
    ContrapontoGitSettings contrapontoGitSettings;

    private User persistUserLinkedToUpstream(Path upstream, String branch) {
        int token = java.util.concurrent.ThreadLocalRandom.current().nextInt();
        User u = Given.user()
                      .withUsername("g_async_%06x".formatted(Integer.toUnsignedLong(token) & 0xFFFFFFL))
                      .withEmail("g_async_%08x@t.st".formatted(Integer.toUnsignedLong(token)))
                      .withName("Git Async Tester")
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
    void runScheduledImportWithoutOuterTransactionRecordsSuccessfulRun() throws Exception {
        Path upstream = Files.createTempDirectory("git-async-upstream-");
        String branch = bootstrapUpstreamRepo(upstream);
        User user = persistUserLinkedToUpstream(upstream, branch);
        long blogId = blogId(user);

        integrationTransaction.runScheduledImport(blogId, GitSyncTrigger.BLOG_SAVE_WARMUP);

        var runs = Given.transaction(() -> gitSyncRunService.listForBlog(blogId, PageQuery.forGrid(20, 1)));
        assertThat(runs.data()).hasSize(1);
        GitSyncRun run = runs.data().get(0);
        assertThat(run.getOperation()).isEqualTo(GitSyncOperation.IMPORT);
        assertThat(run.getOutcome()).isEqualTo(GitSyncOutcome.SUCCESS);
        assertThat(run.getErrorDetail()).isNull();

        Optional<Post> imported = Given.transaction(() -> postRepository.findByBlogIdAndSlugWithTags(blogId, "async-mirror"));
        assertThat(imported).isPresent();
        assertThat(imported.get().getTitle()).isEqualTo("Async Mirror");
    }
}
