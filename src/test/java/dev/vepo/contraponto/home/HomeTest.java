package dev.vepo.contraponto.home;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.openqa.selenium.By.className;
import static org.openqa.selenium.By.cssSelector;
import static org.openqa.selenium.support.ui.ExpectedConditions.invisibilityOfElementLocated;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.locators.RelativeLocator;
import org.openqa.selenium.support.ui.WebDriverWait;

import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebTest;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;

@WebTest
@QuarkusTest
class HomeTest {

    @TestHTTPResource("/")
    URL testUrl;

    private Map<String, User> users;

    @Test
    void authModalOpensFromHomePage(WebDriver driver, WebDriverWait wait) {
        driver.get(testUrl.toString());

        var loginBtn = wait.until(visibilityOfElementLocated(className("auth-btn-login")));
        loginBtn.click();

        // Modal should become visible
        var modal = wait.until(visibilityOfElementLocated(By.id("authModal")));
        assertThat(modal.isDisplayed()).isTrue();

        // Modal should contain login form
        var loginInput = modal.findElement(cssSelector("input[name='login']"));
        var passwordInput = modal.findElement(cssSelector("input[name='password']"));
        var submitBtn = modal.findElement(cssSelector("button[type='submit']"));

        assertThat(loginInput.isDisplayed()).isTrue();
        assertThat(passwordInput.isDisplayed()).isTrue();
        assertThat(submitBtn.isDisplayed()).isTrue();

        // Close modal
        var closeBtn = modal.findElement(className("modal__close"));
        closeBtn.click();
        wait.until(invisibilityOfElementLocated(By.id("authModal")));
    }

    @Test
    void clickingAuthorLinkNavigatesToUserBlog(WebDriver driver, WebDriverWait wait) {
        driver.get(testUrl.toString());

        // Find the first article card after the featured post, get the author link
        WebElement firstCard = wait.until(visibilityOfElementLocated(cssSelector(".article-card")));
        WebElement authorLink = firstCard.findElement(cssSelector(".article-meta__author"));
        String expectedUserName = authorLink.getText(); // assume username matches author name
        // Actually the link's href or data-hx-get attribute contains the username
        String hxGet = driver.findElement(RelativeLocator.with(By.cssSelector("[data-hx-get"))
                                                         .near(authorLink))
                             .getAttribute("data-hx-get");
        assertThat(hxGet).isNotNull();

        authorLink.click();

        assertThat(users.containsKey(expectedUserName)).isTrue();
        var username = users.get(expectedUserName).getUsername();

        // Wait for the user blog page to load – the main content should contain the
        // user's name or blog header
        wait.until(d -> d.getCurrentUrl().contains("/" + username));
        WebElement blogHeader = wait.until(visibilityOfElementLocated(cssSelector(".user-blog__name")));
        assertThat(blogHeader.getText()).isEqualToIgnoringCase(expectedUserName);
    }

    @Test
    void clickingOnPostLinkNavigatesWithHtmx(WebDriver driver, WebDriverWait wait) {
        driver.get(testUrl.toString());

        // Find first article card (non-featured)
        var firstCard = wait.until(visibilityOfElementLocated(cssSelector(".article-card")));
        var titleLink = firstCard.findElement(cssSelector(".article-card__title a"));

        String hxGet = titleLink.getAttribute("data-hx-get");
        assertThat(hxGet).isNotNull();

        // Click the link (it uses htmx, so the main content should update)
        titleLink.click();

        // Wait for the new content to settle (main element should still be present)
        wait.until(d -> driver.findElement(By.tagName("main")).isDisplayed());

        // After navigation, the URL should have changed to the post slug
        await().until(() -> driver.getCurrentUrl().contains("/post/"));
    }

    @Test
    void featuredPostIsDisplayedWhenExists(WebDriver driver, WebDriverWait wait) {
        driver.get(testUrl.toString());

        // If there is at least one post, the featured section should be present
        var featuredSection = driver.findElements(className("featured"));
        if (!featuredSection.isEmpty()) {
            var featured = featuredSection.get(0);
            assertThat(featured.isDisplayed()).isTrue();

            var featuredTitle = featured.findElement(className("featured__title"));
            assertThat(featuredTitle.getText()).isNotBlank();

            var featuredLink = featuredTitle.findElement(By.tagName("a"));
            assertThat(featuredLink.getAttribute("data-hx-get")).isNotNull();
        }
    }

    @Test
    void homePageLoadsSuccessfully(WebDriver driver, WebDriverWait wait) {
        driver.get(testUrl.toString());

        // Header should be present
        var header = wait.until(visibilityOfElementLocated(className("site-header")));
        assertThat(header.isDisplayed()).isTrue();

        // Login button should be present
        var loginBtn = driver.findElement(className("auth-btn-login"));
        assertThat(loginBtn.isDisplayed()).isTrue();

        // Main content area should be present
        var main = driver.findElement(By.tagName("main"));
        assertThat(main.isDisplayed()).isTrue();

        // If there are posts, the featured section or post grid should exist
        // We'll check for at least one article container
        var articles = driver.findElements(cssSelector(".article-card, .featured"));
        assertThat(articles.isEmpty()).isFalse();
    }

    @Test
    void loadMoreButtonAppearsWhenManyPosts(WebDriver driver, WebDriverWait wait) {
        driver.get(testUrl.toString());

        // The button is present only if there are more than 6 posts
        var loadMore = driver.findElements(cssSelector(".load-more .btn"));
        if (!loadMore.isEmpty()) {
            assertThat(loadMore.get(0).isDisplayed()).isTrue();
            assertThat(loadMore.get(0).getText()).contains("Load more");
        }
    }

    @Test
    void loadMoreButtonFetchesNextPageOfPosts(WebDriver driver, WebDriverWait wait) {
        driver.get(testUrl.toString());

        // Ensure we have at least 13 posts to trigger pagination (page size = 12)
        // The setup already creates 8 posts – we need more. We'll add them in the
        // setup.
        // For this test, we verify the button exists.
        WebElement loadMoreBtn = wait.until(visibilityOfElementLocated(cssSelector(".load-more .btn")));
        assertThat(loadMoreBtn.isDisplayed()).isTrue();

        // Count initial posts in the grid after the first page loads
        // The grid initially contains 11 posts (first page: featured + 11 grid posts =
        // 12 total)
        // Wait for the page to settle
        await().until(() -> driver.findElements(cssSelector(".posts-grid .article-card")).size() > 0);
        int initialCount = driver.findElements(cssSelector(".posts-grid .article-card")).size();

        // Click "Load more"
        loadMoreBtn.click();

        // Wait for new posts to appear (the load‑more button should be replaced by new
        // content)
        wait.until(invisibilityOfElementLocated(cssSelector(".load-more .btn")));

        // New posts should have been added; the container with id "more-posts" is
        // replaced
        // The grid now contains both initial and new posts.
        int newCount = driver.findElements(cssSelector(".posts-grid .article-card")).size();
        assertThat(newCount).isGreaterThan(initialCount);
    }

    @Test
    void postGridDisplaysArticles(WebDriver driver, WebDriverWait wait) {
        driver.get(testUrl.toString());

        var postGrid = wait.until(visibilityOfElementLocated(className("posts-grid")));
        assertThat(postGrid.isDisplayed()).isTrue();

        var articleCards = postGrid.findElements(className("article-card"));
        // If there are posts, at least one card should exist (excluding featured)
        if (articleCards.size() > 0) {
            var firstCard = articleCards.get(0);
            var title = firstCard.findElement(className("article-card__title"));
            assertThat(title.getText()).isNotBlank();

            var link = title.findElement(By.tagName("a"));
            assertThat(link.getAttribute("data-hx-get")).contains("/post/");
        }
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
                          .withTitle("Post " + index)
                          .withSlug("post-" + index)
                          .withDescription("Description for post " + index)
                          .withContent(baseContent)
                          .withAuthor(author)
                          .withCover(Given.randomCover())
                          .persist();
                 });
    }
}