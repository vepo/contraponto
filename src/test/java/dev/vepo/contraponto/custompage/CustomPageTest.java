package dev.vepo.contraponto.custompage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebTest;
import dev.vepo.contraponto.user.Role;

@WebTest
class CustomPageTest {
    @Test
    void accessGlobalPage(App app) {
        app.access()
           .assertLinks(PagePlacement.FOOTER, "/page-1")
           .click(PagePlacement.FOOTER, "/page-1")
           .assertUrl("/page-1")
           .assertLinks(PagePlacement.FOOTER, "/page-1");
    }

    @Test
    void accessUsersPage(App app) {
        app.access()
           .accessUserBlog("regular-user")
           .assertLinks(PagePlacement.FOOTER, "/page-2", "/page-1")
           .click(PagePlacement.FOOTER, "/page-1")
           .assertUrl("/page-1")
           .assertLinks(PagePlacement.FOOTER, "/page-2", "/page-1");
    }

    @BeforeEach
    void setup() {
        Given.cleanup();
        var regularUser = Given.user()
                               .withUsername("regular-user")
                               .withName("Regular User")
                               .withEmail("regular-user@contraponto.com.br")
                               .withPassword("qwas1234")
                               .withRole(Role.USER)
                               .persist();
        Given.customPage()
             .withSlug("/page-1")
             .withTitle("Page 1")
             .withContent("Page 1 content")
             .withPlacement(PagePlacement.FOOTER)
             .withSection("Section 1")
             .persist();

        Given.customPage()
             .withSlug("/page-2")
             .withTitle("Page 2")
             .withContent("Page 2 content")
             .withPlacement(PagePlacement.FOOTER)
             .withSection("Section 2")
             .withBlog(regularUser)
             .persist();
    }
}
