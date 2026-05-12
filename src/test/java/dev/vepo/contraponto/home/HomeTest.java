package dev.vepo.contraponto.home;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebTest;
import dev.vepo.contraponto.user.User;

@WebTest
class HomeTest {

    private Map<String, User> users;

    @Test
    void authModalOpensFromHomePage(App app) {
        app.access()
           .loginModal()
           .assertModalIsOpen()
           .closeModal()
           .assertModalWasClosed();
    }

    @Test
    void clickingAuthorLinkNavigatesToUserBlog(App app) {
        app.access()
           // Find the first article card after the featured post, get the author link
           .featuredCard()
           .assertAuthorName("Author 15")
           // Actually the link's href or data-hx-get attribute contains the username
           .accessAuthorBlog()
           // Wait for the user blog page to load – the main content should contain the
           // user's name or blog header
           .assertBlogName("Author 15");
    }

    @Test
    void clickingOnPostLinkNavigatesWithHtmx(App app) {
        app.access()
           // Find first article card (non-featured)
           .clickFirstPostTitle()
           .waitForReady()
           .assertUrlContains("/post/");
    }

    @Test
    void featuredPostIsDisplayedWhenExists(App app) {
        app.access()
           // If there is at least one post, the featured section should be present
           .featuredCard()
           .assertTitle()
           .assertLink();

    }

    @Test
    void homePageLoadsSuccessfully(App app) {
        app.access()
           // Header should be present
           .assertHeaderIsDisplayed()
           // Login button should be present
           .assertAccessButtonIsDisplayed()
           // Main content area should be present
           .assertMainContent()
           // If there are posts, the featured section or post grid should exist
           // We'll check for at least one article container
           .assertFeaturedDisplayed()
           .assertNumberOfPosts(13) // featured + 12
           .assertLoadMoreIsVisible()
           .loadMore()
           .assertNumberOfPosts(15) // all posts
           .assertLoadMoreIsNotVisible();
    }

    @Test
    void loadMoreButtonFetchesNextPageOfPosts(App app) {
        app.access()
           // Ensure we have at least 13 posts to trigger pagination (page size = 12)
           // The setup already creates 8 posts – we need more. We'll add them in the
           // setup.
           .assertNumberOfPosts(13)
           .assertPostTitles(IntStream.range(3, 16)
                                      .mapToObj(this::titleFor)
                                      .toList()
                                      .reversed())
           .assertLoadMoreIsVisible()
           .loadMore()
           .assertNumberOfPosts(15)
           .assertPostTitles(IntStream.range(1, 16)
                                      .mapToObj(this::titleFor)
                                      .toList()
                                      .reversed());
    }

    // Update the setup method to create enough posts for pagination (at least 13)
    @BeforeEach
    void setup() {
        users = new HashMap<>();
        Given.cleanup();

        // Create a test user (for modal tests)
        Given.user()
             .withUsername("homeuser")
             .withEmail("home@example.com")
             .withPassword("homepass123")
             .withName("Home Tester")
             .persist();

        // Create 15 users and posts to guarantee pagination (page size = 12, need >12
        // to show button)
        IntStream.range(1, 16)
                 .forEach(index -> {
                     var author = Given.user()
                                       .withUsername("user-" + index)
                                       .withEmail("user-" + index + "@contraponto.com.br")
                                       .withName("Author " + index)
                                       .withPassword("homepass123")
                                       .persist();
                     users.put(author.getName(), author);

                     var baseContent = """
                                       Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut
                                       labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco
                                       laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in
                                       voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat
                                       non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
                                       """;

                     Given.post()
                          .withTitle(titleFor(index))
                          .withSlug("post-" + index)
                          .withDescription("Description for post " + index)
                          .withContent(baseContent)
                          .withAuthor(author)
                          .withFeatured(true)
                          .withCover(Given.randomCover())
                          .persist();
                 });
    }

    private String titleFor(int index) {
        return "Post " + index;
    }
}