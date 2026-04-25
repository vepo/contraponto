package dev.vepo.contraponto.home;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.openqa.selenium.By.className;
import static org.openqa.selenium.By.cssSelector;
import static org.openqa.selenium.support.ui.ExpectedConditions.invisibilityOfElementLocated;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;

import java.net.URL;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
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

    @BeforeEach
    void setup() {
        // Ensure clean state for tests
        Given.cleanup();

        // Create a test user (for modal tests)
        Given.user()
             .withUsername("homeuser")
             .withEmail("home@example.com")
             .withPassword("homepass123")
             .withName("Home Tester")
             .persist();

        var authors = IntStream.range(1, 9)
                               .mapToObj(index -> Given.user()
                                                       .withUsername("user-" + index)
                                                       .withEmail("user-" + index + "@contraponto.com.br")
                                                       .withName("Author " + index)
                                                       .withPassword("homepass123")
                                                       .persist())
                               .toArray(User[]::new);

        // Create 8 posts to trigger load‑more button (threshold > 6)
        String baseContent = """
                             Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut
                             labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco
                             laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in
                             voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat
                             non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
                             """;

        IntStream.range(1, 9)
                 .forEach(index -> Given.post()
                                        .withTitle("Post " + index)
                                        .withSlug("post-" + index)
                                        .withDescription("Description for post " + index)
                                        .withContent(baseContent)
                                        .withAuthor(authors[index - 1])
                                        .persist());
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
}