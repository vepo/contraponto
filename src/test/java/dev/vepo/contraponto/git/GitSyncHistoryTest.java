package dev.vepo.contraponto.git;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebTest;
import dev.vepo.contraponto.user.User;
import jakarta.inject.Inject;

@WebTest
class GitSyncHistoryTest {

    @Inject
    GitSyncRunService gitSyncRunService;

    @Inject
    BlogRepository blogRepository;

    private User author;
    private Blog blog;

    @Test
    void ownerCanViewGitSyncHistory(App app) {
        long runId = gitSyncRunService.beginRunForBlog(blog.getId(), GitSyncOperation.EXPORT, GitSyncTrigger.PUBLISH, null);
        gitSyncRunService.finalizeRun(runId, new GitSyncRunResult(
                                                                  GitSyncOutcome.SUCCESS,
                                                                  GitErrorKind.NONE,
                                                                  true,
                                                                  true,
                                                                  "abc",
                                                                  "{\"config_source\":\"defaults\"}",
                                                                  null,
                                                                  "Git export succeeded for post \"demo\".",
                                                                  null));

        app.login(author);
        app.goToGitSyncHistory(blog.getId())
           .assertGitSyncHistoryTitle()
           .assertRunListed("Git export succeeded");
    }

    @Test
    void ownerCanViewGitSyncRunDetail(App app) {
        long runId = gitSyncRunService.beginRunForBlog(blog.getId(), GitSyncOperation.IMPORT, GitSyncTrigger.REMOTE_POLL, null);
        gitSyncRunService.finalizeRun(runId, new GitSyncRunResult(
                                                                  GitSyncOutcome.FAILED,
                                                                  GitErrorKind.AUTHENTICATION,
                                                                  false,
                                                                  false,
                                                                  null,
                                                                  null,
                                                                  null,
                                                                  "Git import failed.",
                                                                  "401 Unauthorized"));

        app.login(author);
        app.goToGitSyncRun(blog.getId(), runId)
           .assertDetailShows("Data loadable")
           .assertDetailShows("Repository readable");
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        author = Given.user()
                      .withUsername("gitsyncauthor")
                      .withEmail("gitsync@test.com")
                      .withName("Git Sync Author")
                      .withPassword("Password123!")
                      .persist();
        blog = author.getDefaultBlog();
        blog.setGitEnabled(true);
        blog.setGitRemoteUrl("https://git.example.com/demo/site.git");
        blog.setGitBranch("main");
        Given.transaction(() -> blogRepository.save(blog));
    }
}
