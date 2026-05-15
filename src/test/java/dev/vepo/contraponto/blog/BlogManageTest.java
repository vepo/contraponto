package dev.vepo.contraponto.blog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebTest;
import dev.vepo.contraponto.user.User;

@WebTest
class BlogManageTest {

    private static final String USER_EMAIL = "blogmanage@example.com";
    private static final String USER_PASSWORD = "blogManagePass123";
    private static final String USER_USERNAME = "blogmanageuser";
    private static final String USER_NAME = "Blog Manage Tester";

    private User testUser;

    @Test
    void authenticatedUserCanViewBlogList(App app) {
        app.login(testUser)
           .blogs()
           .assertTitle("My Blogs")
           .assertBlogCount(1)
           .assertBlogListed(USER_NAME, "/" + USER_USERNAME);
    }

    @Test
    void createSecondaryBlogSuccessfully(App app) {
        app.login(testUser)
           .blogs()
           .clickNewBlog()
           .assertTitle("New Blog")
           .fillName("Travel Blog")
           .fillSlug("travel")
           .fillDescription("Trips and notes")
           .assertSubmitEnabled()
           .submit()
           .assertUrl("/blogs")
           .assertToastSuccess("Blog saved successfully")
           .assertBlogCount(2)
           .assertBlogListed("Travel Blog", "/" + USER_USERNAME + "/travel")
           .openPublicBlog("/" + USER_USERNAME + "/travel")
           .assertUrl("/" + USER_USERNAME + "/travel");
    }

    @Test
    void deactivateSecondaryBlog(App app) {
        Given.blog()
             .withUser(testUser)
             .withSlug("archive")
             .withName("Archive Blog")
             .withDescription("To archive")
             .persist();

        app.login(testUser)
           .blogs()
           .assertBlogCount(2)
           .clickDeactivate("Archive Blog")
           .assertUrl("/blogs")
           .assertToastSuccess("Blog saved successfully")
           .assertBlogCount(2);
    }

    @Test
    void defaultBlogCannotBeDeactivatedFromList(App app) {
        app.login(testUser)
           .blogs()
           .assertDeactivateNotAvailableOnList(USER_NAME);
    }

    @Test
    void defaultBlogEditFormHasNoDeactivateButton(App app) {
        app.login(testUser)
           .blogs()
           .clickEdit(USER_NAME)
           .assertTitle("Edit Blog")
           .assertDeactivateButtonNotPresent();
    }

    @Test
    void editSecondaryBlog(App app) {
        Given.blog()
             .withUser(testUser)
             .withSlug("tech")
             .withName("Tech Blog")
             .withDescription("Old description")
             .persist();

        app.login(testUser)
           .blogs()
           .clickEdit("Tech Blog")
           .fillName("Tech and Code")
           .fillSlug("tech")
           .fillDescription("Updated description")
           .assertSubmitEnabled()
           .submit()
           .assertUrl("/blogs")
           .assertToastSuccess("Blog saved successfully")
           .assertBlogListed("Tech and Code", "/" + USER_USERNAME + "/tech");
    }

    @Test
    void newBlogFormShowsValidationErrors(App app) {
        app.login(testUser)
           .newBlog()
           .assertTitle("New Blog")
           .assertNameEmpty()
           .assertSlugEmpty()
           .assertSubmitDisabled()
           .fillSlug("ab")
           .clearSlug()
           .assertFieldError("Slug is required.");
    }

    @Test
    void newBlogFormStartsEmptyWithSubmitDisabled(App app) {
        app.login(testUser)
           .blogs()
           .clickNewBlog()
           .assertTitle("New Blog")
           .assertNameEmpty()
           .assertSlugEmpty()
           .assertSubmitDisabled();
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
    }

    @Test
    void unauthenticatedUserCannotAccessBlogs(App app) {
        app.access()
           .blogs()
           .assertManagePageNotLoaded();
    }
}
