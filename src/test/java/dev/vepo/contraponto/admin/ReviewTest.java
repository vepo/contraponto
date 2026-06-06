package dev.vepo.contraponto.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebTest;
import dev.vepo.contraponto.user.Role;
import dev.vepo.contraponto.user.User;

@WebTest
class ReviewTest {

    private User admin;
    private User regularUser;
    private User editorUser;
    private Post mainPost1;
    private Post mainPost2;
    private Post blogPost1;
    private Post blogPost2;

    @Test
    void checkPromoteBlogPostToFeatureButtonTest(App app) {
        app.access()
           .login(admin)
           .goTo(blogPost1)
           .assertFeaturedButtonIsPresent()
           .toggleFeatured()
           .home()
           .assertNumberOfPosts(1)
           .logout()
           .login(regularUser)
           .goTo(blogPost2)
           .assertFeaturedButtonIsNotPresent()
           .home()
           .logout()
           .login(editorUser)
           .goTo(blogPost2)
           .assertFeaturedButtonIsPresent();
    }

    @Test
    void checkPromoteMainBlogPostToFeatureButtonTest(App app) {
        app.access()
           .login(admin)
           .goTo(mainPost1)
           .assertFeaturedButtonIsPresent()
           .toggleFeatured()
           .home()
           .assertNumberOfPosts(1)
           .logout()
           .login(regularUser)
           .goTo(mainPost2)
           .assertFeaturedButtonIsNotPresent()
           .home()
           .logout()
           .login(editorUser)
           .goTo(mainPost2)
           .assertFeaturedButtonIsPresent();
    }

    @Test
    void reviewPageUsesManagePagination(App app) {
        for (int i = 5; i <= 25; i++) {
            Given.post()
                 .withAuthor(regularUser)
                 .withTitle("Published Post " + i)
                 .withSlug("published-post-" + i)
                 .withContent("Content " + i)
                 .withPublished(true)
                 .persist();
        }

        app.access()
           .login(editorUser)
           .goToReview()
           .assertNumberOfPosts(20)
           .assertManagePaginationSummary(25, 1, 2)
           .goToManageNextPage()
           .assertNumberOfPosts(5)
           .assertManagePaginationSummary(25, 2, 2)
           .assertLoadMoreIsNotVisible();
    }

    @Test
    void reviewRowLinksOpenPostInNewTab(App app) {
        app.access()
           .login(editorUser)
           .goToReview()
           .assertPostOpenLink(mainPost1);
    }

    @BeforeEach
    void setup() {
        Given.cleanup();
        admin = Given.user()
                     .withUsername("admin")
                     .withName("Admin")
                     .withEmail("admin@contraponto.com.br")
                     .withPassword("qwas1234")
                     .withRole(Role.ADMIN)
                     .persist();
        regularUser = Given.user()
                           .withUsername("regular-user")
                           .withName("Regular User")
                           .withEmail("regular-user@contraponto.com.br")
                           .withPassword("qwas1234")
                           .withRole(Role.USER)
                           .persist();
        var blog = Given.blog()
                        .withUser(regularUser)
                        .withSlug("secondary")
                        .withName("Secondary Blog")
                        .withDescription("Secondary Blog description")
                        .persist();
        editorUser = Given.user()
                          .withUsername("editor-user")
                          .withName("Editor User")
                          .withEmail("editor-user@contraponto.com.br")
                          .withPassword("qwas1234")
                          .withRole(Role.EDITOR)
                          .persist();
        mainPost1 = Given.post()
                         .withAuthor(regularUser)
                         .withTitle("Post 1")
                         .withSlug("post-1")
                         .withContent("Post 1 content")
                         .withCover(Given.randomCover(regularUser.getDefaultBlog()))
                         .withDescription("Post 1 description")
                         .withPublished(true)
                         .withFeatured(false)
                         .persist();
        mainPost2 = Given.post()
                         .withAuthor(regularUser)
                         .withTitle("Post 2")
                         .withSlug("post-2")
                         .withContent("Post 2 content")
                         .withCover(Given.randomCover(regularUser.getDefaultBlog()))
                         .withDescription("Post 2 description")
                         .withPublished(true)
                         .withFeatured(false)
                         .persist();
        blogPost1 = Given.post()
                         .withBlog(blog)
                         .withTitle("Post 1")
                         .withSlug("post-1")
                         .withContent("Post 1 content")
                         .withCover(Given.randomCover(blog))
                         .withDescription("Post 1 description")
                         .withPublished(true)
                         .withFeatured(false)
                         .persist();
        blogPost2 = Given.post()
                         .withBlog(blog)
                         .withTitle("Post 2")
                         .withSlug("post-2")
                         .withContent("Post 2 content")
                         .withCover(Given.randomCover(blog))
                         .withDescription("Post 2 description")
                         .withPublished(true)
                         .withFeatured(false)
                         .persist();
    }

    @Test
    void toggleFeaturedKeepsEnglishLocaleOnSwappedRow(App app) {
        app.access()
           .login(admin)
           .setLocaleCookie("en")
           .refresh()
           .goToReview()
           .toggleFeatured(mainPost1)
           .assertPostFeatured(mainPost1, true)
           .assertPostFeaturedToggleLabel(mainPost1, "Featured");
    }

    @Test
    void toggleFeaturedTest(App app) {
        app.access()
           .login(admin)
           .assertNumberOfPosts(0)
           .goToReview()
           .assertNumberOfPosts(4)
           .assertManagePaginationVisible()
           .toggleFeatured(mainPost1)
           .assertPostFeatured(mainPost1, true)
           .home()
           .assertNumberOfPosts(1);
    }
}
