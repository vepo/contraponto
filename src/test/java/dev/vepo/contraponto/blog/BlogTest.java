package dev.vepo.contraponto.blog;

import java.net.URL;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebTest;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResource;
import jakarta.ws.rs.core.Response;

@WebTest
class BlogTest {

    @TestHTTPResource("/")
    URL testUrl;

    private User testAuthor;
    private User nonExistentAuthor;

    @Test
    void clickingPostOnUserBlogNavigatesToPostPage(App app) {
        app.access()
           .goTo(testAuthor.getDefaultBlog())
           // Click on the first post in the grid (non‑featured)
           .clickFirstPostTitle()
           // Should land on the post page
           .assertUrlContains("/post/")
           .assertPostTitle("Author Post 14"); // 15 is the featured
    }

    @Test
    void nonExistentUserReturns404(App app) {
        app.goTo(nonExistentAuthor.getDefaultBlog())
           // The error page should be displayed (status 404)
           .assertErrorPage(Response.Status.NOT_FOUND);
    }

    @BeforeEach
    void setup() {
        Given.cleanup();

        testAuthor = Given.user()
                          .withUsername("blogauthor")
                          .withEmail("author@example.com")
                          .withPassword("authorpass")
                          .withName("Blog Author")
                          .persist();
        nonExistentAuthor = Given.user()
                                 .withUsername("non-existent-author")
                                 .withEmail("non-existent-author@example.com")
                                 .withPassword("non-existent-author")
                                 .withName("Blog Non Existent Author")
                                 .get();

        String baseContent = """
                             Content of the blog post. Lorem ipsum dolor sit amet.
                             """;

        // Create 15 posts for this author to test pagination (page size = 12)
        IntStream.range(1, 16)
                 .forEach(i -> Given.post()
                                    .withTitle("Author Post " + i)
                                    .withSlug("author-post-" + i)
                                    .withDescription("Description " + i)
                                    .withContent(baseContent)
                                    .withAuthor(testAuthor)
                                    .persist());
    }

    @Test
    void userBlogHasCorrectPageTitle(App app) {
        app.access()
           .goTo(testAuthor.getDefaultBlog())
           .assertPageTitleContains(testAuthor.getName(), "contraponto");
    }

    @Test
    void userBlogLoadMoreButtonWorks(App app) {
        app.access()
           .goTo(testAuthor.getDefaultBlog())
           .assertNumberOfPosts(13)
           .assertLoadMoreIsVisible()
           .loadMore()
           .assertNumberOfPosts(15)
           .assertLoadMoreIsNotVisible();
    }

    @Test
    void userBlogPageLoadsSuccessfully(App app) {
        app.access()
           .goTo(testAuthor.getDefaultBlog())
           .featuredCard()
           .assertAuthorName(testAuthor.getName())
           .assertTitle("Author Post 15")
           .accessPost()
           .assertUrlContains("/post/");
    }

    @Test
    void userBlogShowsEmptyStateWhenNoPosts(App app) {
        // Create a new user with no posts
        User emptyUser = Given.user()
                              .withUsername("emptyuser")
                              .withEmail("empty@example.com")
                              .withPassword("emptypass")
                              .withName("Empty User")
                              .persist();
        app.access()
           .goTo(emptyUser.getDefaultBlog())
           .assertNoFeaturedCard()
           .assertNotPostMessage();

    }
}