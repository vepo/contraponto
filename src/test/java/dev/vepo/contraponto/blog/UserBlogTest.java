package dev.vepo.contraponto.blog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openqa.selenium.By.cssSelector;
import static org.openqa.selenium.support.ui.ExpectedConditions.invisibilityOfElementLocated;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;

import java.net.URL;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebTest;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;

@WebTest
@QuarkusTest
class UserBlogTest {

    @TestHTTPResource("/")
    URL testUrl;

    private User testAuthor;
    private String testUsername;
    private String testName;

    @Test
    void clickingPostOnUserBlogNavigatesToPostPage(WebDriver driver, WebDriverWait wait) {
        driver.get(testUrl.toString() + testUsername);

        // Click on the first post in the grid (non‑featured)
        WebElement firstCard = wait.until(visibilityOfElementLocated(cssSelector(".article-card")));
        WebElement titleLink = firstCard.findElement(cssSelector(".article-card__title a"));
        String expectedSlug = titleLink.getAttribute("data-hx-get");

        titleLink.click();

        // Should land on the post page
        wait.until(d -> d.getCurrentUrl().contains("/post/"));
        WebElement postTitle = wait.until(visibilityOfElementLocated(cssSelector(".article-page__title")));
        assertThat(postTitle.getText()).isNotBlank();
    }

    @Test
    void nonExistentUserReturns404(WebDriver driver, WebDriverWait wait) {
        driver.get(testUrl.toString() + "nonexistentuser123");

        // The error page should be displayed (status 404)
        wait.until(visibilityOfElementLocated(cssSelector(".error-page")));
        WebElement errorCode = driver.findElement(cssSelector(".error-code"));
        assertThat(errorCode.getText()).contains("404");
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
        testUsername = testAuthor.getUsername();
        testName = testAuthor.getName();

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
    void userBlogHasCorrectPageTitle(WebDriver driver, WebDriverWait wait) {
        driver.get(testUrl.toString() + testUsername);

        String pageTitle = driver.getTitle();
        assertThat(pageTitle).contains(testName);
        assertThat(pageTitle).contains("contraponto");
    }

    @Test
    void userBlogLoadMoreButtonWorks(WebDriver driver, WebDriverWait wait) {
        driver.get(testUrl.toString() + testUsername);

        // Wait for the initial posts to load
        wait.until(visibilityOfElementLocated(cssSelector(".posts-grid")));
        int initialCardCount = driver.findElements(cssSelector(".posts-grid .article-card")).size();

        // Load more button should be visible because we have >12 posts
        WebElement loadMoreBtn = wait.until(visibilityOfElementLocated(cssSelector(".load-more .btn")));
        assertThat(loadMoreBtn.isDisplayed()).isTrue();

        loadMoreBtn.click();

        // The button disappears after loading more (it is replaced by the next page's
        // button)
        wait.until(invisibilityOfElementLocated(cssSelector(".load-more .btn")));

        // New posts should have been added
        int newCardCount = driver.findElements(cssSelector(".posts-grid .article-card")).size();
        assertThat(newCardCount).isGreaterThan(initialCardCount);
    }

    @Test
    void userBlogPageLoadsSuccessfully(WebDriver driver, WebDriverWait wait) {
        driver.get(testUrl.toString() + testUsername);

        // Header should be present
        WebElement blogName = wait.until(visibilityOfElementLocated(cssSelector(".user-blog__name")));
        assertThat(blogName.getText()).isEqualTo(testName);

        // There should be at least one post in the grid
        WebElement postGrid = wait.until(visibilityOfElementLocated(cssSelector(".posts-grid")));
        List<WebElement> articles = postGrid.findElements(cssSelector(".article-card"));
        assertThat(articles).isNotEmpty();

        // The first (featured) post should be prominent
        WebElement featured = driver.findElement(cssSelector(".featured"));
        assertThat(featured.isDisplayed()).isTrue();
        WebElement featuredTitle = featured.findElement(cssSelector(".featured__title"));
        assertThat(featuredTitle.getText()).contains("Author Post");
    }

    @Test
    void userBlogShowsEmptyStateWhenNoPosts(WebDriver driver, WebDriverWait wait) {
        // Create a new user with no posts
        User emptyUser = Given.user()
                              .withUsername("emptyuser")
                              .withEmail("empty@example.com")
                              .withPassword("emptypass")
                              .withName("Empty User")
                              .persist();

        driver.get(testUrl.toString() + emptyUser.getUsername());

        // The featured section should not be present, and an empty message should
        // appear
        boolean featuredExists = driver.findElements(cssSelector(".featured")).isEmpty();
        assertThat(featuredExists).isTrue();

        WebElement emptyMessage = wait.until(visibilityOfElementLocated(cssSelector(".user-blog__empty")));
        assertThat(emptyMessage.getText()).contains("No posts published yet");
    }
}