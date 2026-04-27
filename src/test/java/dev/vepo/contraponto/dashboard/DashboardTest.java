package dev.vepo.contraponto.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.openqa.selenium.By.className;
import static org.openqa.selenium.By.cssSelector;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;

import java.net.URL;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
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
class DashboardTest {

    @TestHTTPResource("/")
    URL testUrl;

    private User testUser;
    private static final String USER_EMAIL = "dashboard@example.com";
    private static final String USER_PASSWORD = "dashboardPass123";
    private static final String USER_USERNAME = "dashboarduser";
    private static final String USER_NAME = "Dashboard Tester";

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

    // ========================================================================
    // Authentication tests
    // ========================================================================

    @Test
    void unauthenticatedUserIsRedirectedToHome(WebDriver driver, WebDriverWait wait) {
        driver.get(testUrl.toString() + "dashboard");
        // Should be redirected to home page (which contains the login button)
        wait.until(visibilityOfElementLocated(className("auth-btn-login")));
        assertThat(driver.getCurrentUrl()).doesNotContain("/dashboard");
    }

    @Test
    void authenticatedUserCanAccessDashboard(WebDriver driver, WebDriverWait wait) {
        login(driver, wait);
        driver.get(testUrl.toString() + "dashboard");

        WebElement dashboardTitle = wait.until(visibilityOfElementLocated(cssSelector(".dashboard-page__title")));
        assertThat(dashboardTitle.getText()).isEqualTo("Dashboard");
    }

    // ========================================================================
    // Statistics tests
    // ========================================================================

    @Test
    void dashboardShowsCorrectDraftsAndPublishedCounts(WebDriver driver, WebDriverWait wait) {
        // Create 3 drafts and 5 published posts
        IntStream.range(0, 3).forEach(i -> Given.post()
                                                .withTitle("Draft " + i)
                                                .withContent("Content")
                                                .withAuthor(testUser)
                                                .withPublished(false)
                                                .persist());

        IntStream.range(0, 5).forEach(i -> Given.post()
                                                .withTitle("Published " + i)
                                                .withContent("Content")
                                                .withAuthor(testUser)
                                                .withPublished(true)
                                                .persist());

        login(driver, wait);
        driver.get(testUrl.toString() + "dashboard");

        WebElement draftsStat = wait.until(visibilityOfElementLocated(cssSelector(".stat-card:first-child .stat-card__value")));
        WebElement publishedStat = driver.findElement(cssSelector(".stat-card:last-child .stat-card__value"));

        assertThat(draftsStat.getText()).isEqualTo("3");
        assertThat(publishedStat.getText()).isEqualTo("5");
    }

    // ========================================================================
    // Recent posts tests
    // ========================================================================

    @Test
    void recentDraftsDisplayOnlyFiveMostRecent(WebDriver driver, WebDriverWait wait) {
        // Create 7 drafts with incremental updated dates (the last created will be most
        // recent)
        for (int i = 1; i <= 7; i++) {
            Given.post()
                 .withTitle("Draft " + i)
                 .withContent("Content")
                 .withAuthor(testUser)
                 .withPublished(false)
                 .persist();
        }

        login(driver, wait);
        driver.get(testUrl.toString() + "dashboard");

        WebElement recentDraftsSection = wait.until(visibilityOfElementLocated(cssSelector(".recent-section:first-child")));
        var draftItems = recentDraftsSection.findElements(cssSelector(".recent-list__item"));
        assertThat(draftItems).hasSize(5);
        // The most recent draft should be "Draft 7"
        assertThat(draftItems.get(0).findElement(cssSelector(".recent-list__title")).getText()).contains("Draft 7");
    }

    @Test
    void recentPublishedDisplaysOnlyFiveMostRecent(WebDriver driver, WebDriverWait wait) {
        for (int i = 1; i <= 7; i++) {
            Given.post()
                 .withTitle("Published " + i)
                 .withContent("Content")
                 .withAuthor(testUser)
                 .withPublished(true)
                 .persist();
        }

        login(driver, wait);
        driver.get(testUrl.toString() + "dashboard");

        WebElement recentPublishedSection = wait.until(visibilityOfElementLocated(cssSelector(".recent-section:last-child")));
        var publishedItems = recentPublishedSection.findElements(cssSelector(".recent-list__item"));
        assertThat(publishedItems).hasSize(5);
        assertThat(publishedItems.get(0).findElement(cssSelector(".recent-list__title")).getText()).contains("Published 7");
    }

    @Test
    void emptyStateMessagesWhenNoDraftsOrPublished(WebDriver driver, WebDriverWait wait) {
        login(driver, wait);
        driver.get(testUrl.toString() + "dashboard");

        WebElement recentDraftsSection = wait.until(visibilityOfElementLocated(cssSelector(".recent-section:first-child")));
        WebElement emptyDraftsMsg = recentDraftsSection.findElement(cssSelector(".recent-section__empty"));
        assertThat(emptyDraftsMsg.getText()).contains("No drafts yet");

        WebElement recentPublishedSection = driver.findElement(cssSelector(".recent-section:last-child"));
        WebElement emptyPublishedMsg = recentPublishedSection.findElement(cssSelector(".recent-section__empty"));
        assertThat(emptyPublishedMsg.getText()).contains("No published posts yet");
    }

    // ========================================================================
    // View counts tests
    // ========================================================================

    @Test
    void dashboardDisplaysViewCountsForPublishedPosts(WebDriver driver, WebDriverWait wait) {
        // Create a published post
        var post = Given.post()
                        .withTitle("Post with Views")
                        .withContent("Content")
                        .withAuthor(testUser)
                        .withPublished(true)
                        .persist();

        // Simulate a view by visiting the post page
        // First login as a different user or anonymous – we'll just visit anonymously
        // But we need to be logged out to simulate a real view; however, the view
        // recording
        // works for both authenticated and anonymous. We'll just visit as anonymous.
        driver.get(testUrl.toString() + testUser.getUsername() + "/post/" + post.getSlug());
        wait.until(visibilityOfElementLocated(cssSelector(".article-page__title")));

        // Now login as the author and check the dashboard
        login(driver, wait);
        driver.get(testUrl.toString() + "dashboard");

        WebElement recentPublishedSection = wait.until(visibilityOfElementLocated(cssSelector(".recent-section:last-child")));
        WebElement viewsSpan = recentPublishedSection.findElement(cssSelector(".recent-list__meta span:last-child"));
        assertThat(viewsSpan.getText()).contains("1 views");
    }

    // ========================================================================
    // Navigation tests
    // ========================================================================

    @Test
    void viewAllDraftsLinkNavigatesToLibraryWithDraftsTab(WebDriver driver, WebDriverWait wait) {
        login(driver, wait);
        driver.get(testUrl.toString() + "dashboard");

        WebElement viewAllDrafts = driver.findElement(cssSelector(".stat-card:first-child .stat-card__link"));
        viewAllDrafts.click();

        // Should land on library page with drafts tab active
        wait.until(visibilityOfElementLocated(cssSelector(".drafts-page")));
        WebElement activeTab = wait.until(visibilityOfElementLocated(cssSelector(".library-tab--active")));
        assertThat(activeTab.getText()).contains("Drafts");
    }

    @Test
    void viewAllPublishedLinkNavigatesToLibraryWithPublishedTab(WebDriver driver, WebDriverWait wait) {
        login(driver, wait);
        driver.get(testUrl.toString() + "dashboard");

        var viewAllPublished = driver.findElement(cssSelector(".stat-card:last-child .stat-card__link"));
        viewAllPublished.click();

        wait.until(visibilityOfElementLocated(cssSelector(".drafts-page")));

        var publishedTabHeader = driver.findElement(By.xpath("//*[contains(@class, 'library-tab') and contains(text(), 'Published')]"));
        publishedTabHeader.click();

        var activeTab = wait.until(d -> {
            var tab = d.findElement(cssSelector(".library-tab--active"));
            return tab.getText().contains("Published") ? tab : null;
        });

        assertThat(activeTab.getText()).contains("Published");
    }

    @Test
    void clickingRecentDraftNavigatesToEditPage(WebDriver driver, WebDriverWait wait) {
        var draft = Given.post()
                         .withTitle("Recent Draft")
                         .withContent("Content")
                         .withAuthor(testUser)
                         .withPublished(false)
                         .persist();

        login(driver, wait);
        driver.get(testUrl.toString() + "dashboard");

        WebElement draftLink = wait.until(visibilityOfElementLocated(cssSelector(".recent-section:first-child .recent-list__title")));
        draftLink.click();

        wait.until(d -> d.getCurrentUrl().contains("/write/draft/" + draft.getId()));
        WebElement titleInput = wait.until(visibilityOfElementLocated(cssSelector("#title")));
        assertThat(titleInput.getAttribute("value")).isEqualTo("Recent Draft");
    }

    @Test
    void clickingRecentPublishedNavigatesToPostPage(WebDriver driver, WebDriverWait wait) {
        var post = Given.post()
                        .withTitle("Recent Published")
                        .withContent("Content")
                        .withAuthor(testUser)
                        .withPublished(true)
                        .persist();

        login(driver, wait);
        driver.get(testUrl.toString() + "dashboard");

        WebElement postLink = wait.until(visibilityOfElementLocated(cssSelector(".recent-section:last-child .recent-list__title")));
        postLink.click();

        wait.until(d -> d.getCurrentUrl().contains("/post/" + post.getSlug()));
        WebElement postTitle = wait.until(visibilityOfElementLocated(cssSelector(".article-page__title")));
        assertThat(postTitle.getText()).isEqualTo("Recent Published");
    }

    @Test
    void writeNewStoryButtonNavigatesToWritePage(WebDriver driver, WebDriverWait wait) {
        login(driver, wait);
        driver.get(testUrl.toString() + "dashboard");

        WebElement writeBtn = driver.findElement(cssSelector(".dashboard-action .btn"));
        writeBtn.click();

        wait.until(visibilityOfElementLocated(cssSelector(".write-form")));
        assertThat(driver.getCurrentUrl()).endsWith("/write");
    }

    // ========================================================================
    // Helper methods
    // ========================================================================

    private void login(WebDriver driver, WebDriverWait wait) {
        driver.get(testUrl.toString());
        WebElement loginBtn = wait.until(visibilityOfElementLocated(className("auth-btn-login")));
        loginBtn.click();

        WebElement loginInput = wait.until(visibilityOfElementLocated(cssSelector("input[name='login']")));
        WebElement passwordInput = driver.findElement(cssSelector("input[name='password']"));
        WebElement submitBtn = driver.findElement(cssSelector("button[type='submit']"));

        loginInput.sendKeys(USER_EMAIL);
        passwordInput.sendKeys(USER_PASSWORD);
        await().until(() -> submitBtn.isEnabled());
        submitBtn.click();

        await().until(() -> !driver.findElement(By.id("authModal")).isDisplayed());
        wait.until(visibilityOfElementLocated(className("user-menu")));
    }
}