package dev.vepo.contraponto.custompage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebTest;
import dev.vepo.contraponto.user.Role;
import dev.vepo.contraponto.user.User;

@WebTest
class CustomPageManageTest {

    private static final String USER_EMAIL = "pagemanag@example.com";
    private static final String USER_PASSWORD = "pageManagePass123";
    private static final String USER_USERNAME = "pagemanaguser";
    private static final String USER_NAME = "Page Manage Tester";

    private static final String EDITOR_EMAIL = "pageeditor@example.com";
    private static final String EDITOR_PASSWORD = "pageEditorPass123";
    private static final String EDITOR_USERNAME = "pageeditor";

    private User testUser;
    private User editorUser;

    @Test
    void authenticatedUserCanViewEmptyPageList(App app) {
        app.login(testUser)
           .customPages()
           .assertTitle("Custom Pages")
           .assertPageCount(0);
    }

    @Test
    void createBlogPageSuccessfully(App app) {
        app.login(testUser)
           .customPages()
           .clickNewPage()
           .assertTitle("New Page")
           .fillTitle("About Me")
           .fillSlug("about")
           .fillSection("Info")
           .fillContent("<p>Hello world</p>")
           .submit()
           .assertUrl("/pages")
           .assertToastSuccess("Page saved successfully.")
           .assertPageCount(1)
           .assertPageListed("About Me", "/" + USER_USERNAME + "/page/about")
           .openPublicPage("/" + USER_USERNAME + "/page/about")
           .assertUrl("/" + USER_USERNAME + "/page/about");
    }

    @Test
    void deleteBlogPage(App app) {
        Given.customPage()
             .withBlog(testUser.getDefaultBlog())
             .withSlug("/to-delete")
             .withTitle("Delete Me")
             .withContent("<p>gone</p>")
             .withSection("Legal")
             .withPlacement(PagePlacement.FOOTER)
             .persist();

        app.login(testUser)
           .customPages()
           .assertPageCount(1)
           .clickDelete("Delete Me")
           .assertUrl("/pages")
           .assertToastSuccess("Page deleted.")
           .assertPageCount(0);
    }

    @Test
    void editBlogPage(App app) {
        Given.customPage()
             .withBlog(testUser.getDefaultBlog())
             .withSlug("/contact")
             .withTitle("Contact")
             .withContent("<p>old</p>")
             .withSection("Support")
             .withPlacement(PagePlacement.FOOTER)
             .persist();

        app.login(testUser)
           .customPages()
           .clickEdit("Contact")
           .fillTitle("Contact Us")
           .fillSlug("contact")
           .fillSection("Help")
           .fillContent("<p>updated</p>")
           .submit()
           .assertUrl("/pages")
           .assertToastSuccess("Page saved successfully.")
           .assertPageListed("Contact Us", "/" + USER_USERNAME + "/page/contact");
    }

    @Test
    void editorCanCreateApplicationPage(App app) {
        app.login(editorUser)
           .customPages()
           .clickNewPage()
           .selectApplicationScope()
           .fillTitle("Terms")
           .fillSlug("terms")
           .fillSection("Legal")
           .fillContent("<p>Terms of service</p>")
           .submit()
           .assertUrl("/pages")
           .assertToastSuccess("Page saved successfully.")
           .assertPageListed("Terms", "/page/terms")
           .openPublicPage("/page/terms")
           .assertUrl("/page/terms");
    }

    @BeforeEach
    void setup() {
        Given.cleanup();
        testUser = Given.user()
                        .withUsername(USER_USERNAME)
                        .withEmail(USER_EMAIL)
                        .withPassword(USER_PASSWORD)
                        .withName(USER_NAME)
                        .persist();
        editorUser = Given.user()
                          .withUsername(EDITOR_USERNAME)
                          .withEmail(EDITOR_EMAIL)
                          .withPassword(EDITOR_PASSWORD)
                          .withName("Page Editor")
                          .withRole(Role.EDITOR)
                          .persist();
    }

    @Test
    void unauthenticatedUserCannotAccessPages(App app) {
        app.access()
           .customPages()
           .assertManagePageNotLoaded();
    }
}
