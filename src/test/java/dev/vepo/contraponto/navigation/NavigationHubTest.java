package dev.vepo.contraponto.navigation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebTest;
import dev.vepo.contraponto.user.Role;
import dev.vepo.contraponto.user.User;

@WebTest
class NavigationHubTest {

    private User author;

    @Test
    void authorManageHubHidesPlatformBlogs(App app) {
        app.login(author)
           .openUserMenu()
           .clickMenuLink("/manage")
           .assertUrl("/manage")
           .assertHubNavDoesNotContain("Blogs");
    }

    @Test
    void editorManageHubShowsPlatformBlogs(App app) {
        Given.cleanup();
        var editor = Given.user()
                          .withUsername("manageeditor")
                          .withEmail("manageeditor@test.com")
                          .withName("Manage Editor")
                          .withPassword("password123")
                          .withRole(Role.EDITOR)
                          .persist();

        app.login(editor)
           .openUserMenu()
           .clickMenuLink("/manage")
           .clickHubSection("/manage", "blogs")
           .assertUrl("/manage/blogs")
           .assertBreadcrumb("Manage", "Blogs");
    }

    @Test
    void editorMenuOpensReviewHubWithDefaultSection(App app) {
        Given.cleanup();
        var editor = Given.user()
                          .withUsername("hubeditor")
                          .withEmail("hubeditor@test.com")
                          .withName("Hub Editor")
                          .withPassword("password123")
                          .withRole(Role.EDITOR)
                          .persist();

        app.login(editor)
           .openUserMenu()
           .clickMenuLink("/editor")
           .assertUrl("/editor")
           .assertBreadcrumb("Review", "Featured Posts")
           .clickHubSection("/editor", "tags")
           .assertUrl("/editor/tags")
           .assertBreadcrumb("Review", "Tags");
    }

    @Test
    void legacyTagManageRedirectsToEditorTags(App app) {
        Given.cleanup();
        var editor = Given.user()
                          .withUsername("legacyeditor")
                          .withEmail("legacyeditor@test.com")
                          .withName("Legacy Editor")
                          .withPassword("password123")
                          .withRole(Role.EDITOR)
                          .persist();

        app.login(editor)
           .goToPath("/tags/manage")
           .assertUrl("/editor/tags");
    }

    @Test
    void manageHubReachesDashboard(App app) {
        app.login(author)
           .openUserMenu()
           .clickMenuLink("/manage")
           .assertUrl("/manage")
           .assertBreadcrumb("Manage", "Dashboard");
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        author = Given.user()
                      .withUsername("hubauthor")
                      .withEmail("hubauthor@test.com")
                      .withName("Hub Author")
                      .withPassword("password123")
                      .persist();
    }

    @Test
    void writingHubHasBlogsAndAppearance(App app) {
        app.login(author)
           .openUserMenu()
           .clickMenuLink("/writing")
           .clickHubSection("/writing", "blogs")
           .assertUrl("/writing/blogs")
           .assertBreadcrumb("Writing", "Blogs")
           .clickHubSection("/writing", "appearance")
           .assertUrl("/writing/appearance")
           .assertBreadcrumb("Writing", "Appearance");
    }

    @Test
    void writingHubOpensLibrary(App app) {
        app.login(author)
           .openUserMenu()
           .clickMenuLink("/writing")
           .assertUrl("/writing")
           .assertBreadcrumb("Writing", "Library")
           .assertHubNavDoesNotContain("Write");
    }
}
