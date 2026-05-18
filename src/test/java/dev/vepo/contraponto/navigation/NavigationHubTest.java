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
    void editorSeesReviewHub(App app) {
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
           .clickHubCard("/review")
           .assertUrl("/review")
           .assertBreadcrumb("Review", "Featured Posts");
    }

    @Test
    void manageHubReachesDashboard(App app) {
        app.login(author)
           .openUserMenu()
           .clickMenuLink("/manage")
           .assertUrl("/manage")
           .clickHubCard("/dashboard")
           .assertUrl("/dashboard")
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
    void writingHubListsLibrary(App app) {
        app.login(author)
           .openUserMenu()
           .clickMenuLink("/writing")
           .assertUrl("/writing")
           .clickHubCard("/library")
           .assertUrl("/library")
           .assertBreadcrumb("Writing", "Library");
    }
}
