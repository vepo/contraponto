package dev.vepo.contraponto.navigation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebReaderTest;
import dev.vepo.contraponto.user.User;

@WebReaderTest
class BreadcrumbTest {

    private User author;
    private Post publishedPost;

    @Test
    void authorSeesLibraryBreadcrumb(App app) {
        app.login(author)
           .goToPath("/writing/library")
           .assertBreadcrumb("Writing", "Library");
    }

    @Test
    void guestSeesBlogBreadcrumb(App app) {
        app.access()
           .visitBlog(author.getUsername())
           .assertBreadcrumb("Home", "Crumb Author");
    }

    @Test
    void guestSeesPostBreadcrumb(App app) {
        app.access()
           .visitPost(author.getUsername(), publishedPost.getSlug())
           .assertPageTopPresent()
           .assertBreadcrumb("Home", "Crumb Author", "Breadcrumb Post");
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        author = Given.user()
                      .withUsername("crumbauthor")
                      .withEmail("crumbauthor@test.com")
                      .withName("Crumb Author")
                      .withPassword("password123")
                      .persist();
        publishedPost = Given.post()
                             .withAuthor(author)
                             .withTitle("Breadcrumb Post")
                             .withSlug("breadcrumb-post")
                             .withContent("Post body for breadcrumb test.")
                             .persist();
    }
}
