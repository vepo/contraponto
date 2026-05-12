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

//
    // @Test
    // void homePageLoadsSuccessfully(WebDriver driver, WebDriverWait wait) {
    // driver.get(testUrl.toString());
//
    // // Header should be present
    // var header =
    // wait.until(visibilityOfElementLocated(className("site-header")));
    // assertThat(header.isDisplayed()).isTrue();
//
    // // Login button should be present
    // var loginBtn = driver.findElement(className("auth-btn-login"));
    // assertThat(loginBtn.isDisplayed()).isTrue();
//
    // // Main content area should be present
    // var main = driver.findElement(By.tagName("main"));
    // assertThat(main.isDisplayed()).isTrue();
//
    // // If there are posts, the featured section or post grid should exist
    // // We'll check for at least one article container
    // var articles = driver.findElements(cssSelector(".article-card, .featured"));
    // assertThat(articles.isEmpty()).isFalse();
    // }
//
    // @Test
    // void loadMoreButtonAppearsWhenManyPosts(WebDriver driver, WebDriverWait wait)
    // {
    // driver.get(testUrl.toString());
//
    // // The button is present only if there are more than 6 posts
    // var loadMore = driver.findElements(cssSelector(".load-more .btn"));
    // if (!loadMore.isEmpty()) {
    // assertThat(loadMore.get(0).isDisplayed()).isTrue();
    // assertThat(loadMore.get(0).getText()).contains("Load more");
    // }
    // }
//
    // @Test
    // void loadMoreButtonFetchesNextPageOfPosts(WebDriver driver, WebDriverWait
    // wait) {
    // driver.get(testUrl.toString());
//
    // // Ensure we have at least 13 posts to trigger pagination (page size = 12)
    // // The setup already creates 8 posts – we need more. We'll add them in the
    // // setup.
    // // For this test, we verify the button exists.
    // WebElement loadMoreBtn =
    // wait.until(visibilityOfElementLocated(cssSelector(".load-more .btn")));
    // assertThat(loadMoreBtn.isDisplayed()).isTrue();
//
    // // Count initial posts in the grid after the first page loads
    // // The grid initially contains 11 posts (first page: featured + 11 grid posts
    // =
    // // 12 total)
    // // Wait for the page to settle
    // await().until(() -> driver.findElements(cssSelector(".posts-grid
    // .article-card")).size() > 0);
    // int initialCount = driver.findElements(cssSelector(".posts-grid
    // .article-card")).size();
//
    // // Click "Load more"
    // loadMoreBtn.click();
//
    // // Wait for new posts to appear (the load‑more button should be replaced by
    // new
    // // content)
    // wait.until(invisibilityOfElementLocated(cssSelector(".load-more .btn")));
//
    // // New posts should have been added; the container with id "more-posts" is
    // // replaced
    // // The grid now contains both initial and new posts.
    // int newCount = driver.findElements(cssSelector(".posts-grid
    // .article-card")).size();
    // assertThat(newCount).isGreaterThan(initialCount);
    // }
//
    // @Test
    // void postGridDisplaysArticles(WebDriver driver, WebDriverWait wait) {
    // driver.get(testUrl.toString());
//
    // var postGrid =
    // wait.until(visibilityOfElementLocated(className("posts-grid")));
    // assertThat(postGrid.isDisplayed()).isTrue();
//
    // var articleCards = postGrid.findElements(className("article-card"));
    // // If there are posts, at least one card should exist (excluding featured)
    // if (articleCards.size() > 0) {
    // var firstCard = articleCards.get(0);
    // var title = firstCard.findElement(className("article-card__title"));
    // assertThat(title.getText()).isNotBlank();
//
    // var link = title.findElement(By.tagName("a"));
    // assertThat(link.getAttribute("data-hx-get")).contains("/post/");
    // }
    // }
//
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
                          .withTitle("Post " + index)
                          .withSlug("post-" + index)
                          .withDescription("Description for post " + index)
                          .withContent(baseContent)
                          .withAuthor(author)
                          .withFeatured(true)
                          .withCover(Given.randomCover())
                          .persist();
                 });
    }
}