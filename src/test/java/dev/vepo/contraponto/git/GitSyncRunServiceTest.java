package dev.vepo.contraponto.git;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.notification.NotificationRepository;
import dev.vepo.contraponto.notification.NotificationType;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusIntegrationTest
class GitSyncRunServiceTest {

    private static GitSyncRunResult successResult(String summary) {
        return new GitSyncRunResult(
                                    GitSyncOutcome.SUCCESS,
                                    GitErrorKind.NONE,
                                    true,
                                    true,
                                    "deadbeef",
                                    null,
                                    null,
                                    summary,
                                    null);
    }

    @Inject
    GitSyncRunService gitSyncRunService;

    @Inject
    BlogRepository blogRepository;

    @Inject
    NotificationRepository notificationRepository;

    private Blog blog;

    @Test
    @Transactional
    void exportSuccessCreatesNotification() {
        long runId = gitSyncRunService.beginRunForBlog(blog.getId(), GitSyncOperation.EXPORT, GitSyncTrigger.PUBLISH, null);
        gitSyncRunService.finalizeRun(runId, successResult("export ok"));

        var notifications = notificationRepository.findPage(blog.getOwner().getId(), PageQuery.forGrid(20, 1));
        assertThat(notifications.data()).anyMatch(n -> n.getType() == NotificationType.GIT_SYNC_SUCCEEDED);
    }

    @Test
    @Transactional
    void importFailureCreatesNotification() {
        long runId = gitSyncRunService.beginRunForBlog(blog.getId(), GitSyncOperation.IMPORT, GitSyncTrigger.REMOTE_POLL, null);
        gitSyncRunService.finalizeRun(runId, new GitSyncRunResult(
                                                                  GitSyncOutcome.FAILED,
                                                                  GitErrorKind.NETWORK,
                                                                  false,
                                                                  false,
                                                                  null,
                                                                  null,
                                                                  null,
                                                                  "Git import failed.",
                                                                  "timeout"));

        var notifications = notificationRepository.findPage(blog.getOwner().getId(), PageQuery.forGrid(20, 1));
        assertThat(notifications.data()).anyMatch(n -> n.getType() == NotificationType.GIT_SYNC_FAILED);
    }

    @Test
    @Transactional
    void importSuccessDoesNotCreateNotification() {
        long runId = gitSyncRunService.beginRunForBlog(blog.getId(), GitSyncOperation.IMPORT, GitSyncTrigger.REMOTE_POLL, null);
        gitSyncRunService.finalizeRun(runId, successResult("import ok"));

        var notifications = notificationRepository.findPage(blog.getOwner().getId(), PageQuery.forGrid(20, 1));
        assertThat(notifications.data()).noneMatch(n -> n.getType() == NotificationType.GIT_SYNC_SUCCEEDED);
    }

    @Test
    @Transactional
    void listsRunsForBlog() {
        long runId = gitSyncRunService.beginRunForBlog(blog.getId(), GitSyncOperation.EXPORT, GitSyncTrigger.PUBLISH, null);
        gitSyncRunService.finalizeRun(runId, new GitSyncRunResult(
                                                                  GitSyncOutcome.SUCCESS,
                                                                  GitErrorKind.NONE,
                                                                  true,
                                                                  true,
                                                                  "abc123",
                                                                  "{\"config_source\":\"defaults\"}",
                                                                  null,
                                                                  "Git export succeeded.",
                                                                  null));

        var page = gitSyncRunService.listForBlog(blog.getId(), PageQuery.forGrid(20, 1));
        assertThat(page.data()).hasSize(1);
        assertThat(page.data().get(0).getOutcome()).isEqualTo(GitSyncOutcome.SUCCESS);
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        User owner = Given.user()
                          .withUsername("gitsyncsvc")
                          .withEmail("gitsyncsvc@test.com")
                          .withName("Git Sync Svc")
                          .withPassword("Password123!")
                          .persist();
        blog = owner.getDefaultBlog();
        blog.setGitEnabled(true);
        blog.setGitRemoteUrl("https://git.example.com/demo.git");
        blog.setGitBranch("main");
        Given.transaction(() -> blogRepository.save(blog));
    }
}
