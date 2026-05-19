package dev.vepo.contraponto.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.openqa.selenium.By.className;
import static org.openqa.selenium.By.cssSelector;
import static org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable;
import static org.openqa.selenium.support.ui.ExpectedConditions.invisibilityOfElementLocated;
import static org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlContains;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlToBe;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfAllElementsLocatedBy;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.locators.RelativeLocator;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.components.forms.LoginEndpoint;
import dev.vepo.contraponto.custompage.PagePlacement;
import dev.vepo.contraponto.shared.security.CsrfTokenService;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.shared.infra.LoggedUserProvider;
import dev.vepo.contraponto.shared.infra.TemplateExtensions;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResourceManager;
import jakarta.ws.rs.core.Response.Status;

public class App {

    public abstract class AccessModal<T extends AccessModal<T>> {

        private AccessModal() {}

        public T assertErrorMessage(String errorMessage) {
            var authError = wait.until(visibilityOfElementLocated(cssSelector("#authModal .response.error")));
            assertThat(authError.getText()).contains(errorMessage);
            return (T) this;
        }

        public T assertFieldError(String... errorMessages) {
            var errors = driver.findElements(cssSelector(".form-group .error-message.visible"));
            assertThat(errors).hasSize(errorMessages.length)
                              .allMatch(WebElement::isDisplayed)
                              .extracting(WebElement::getText)
                              .containsExactlyInAnyOrder(errorMessages);
            return (T) this;
        }

        public T assertModalIsOpen() {
            var modal = wait.until(visibilityOfElementLocated(By.className("modal__container")));
            assertThat(modal.isDisplayed()).isTrue();
            return (T) this;
        }

        public App assertModalWasClosed() {
            wait.until(invisibilityOfElementLocated(By.className("modal__container")));
            App.this.waitForReady();
            App.this.syncCsrfCookieFromPage();
            return App.this;
        }

        public T assertNoFieldErrorMessage() {
            assertThat(driver.findElements(cssSelector(".form-group .error-message.visible"))).isEmpty();
            return (T) this;
        }

        public T assertSubmitDisabled() {
            await().until(() -> {
                var found = driver.findElements(cssSelector("#authModal button[type=\"submit\"]"));
                return found.size() == 1 && !found.get(0).isEnabled();
            });
            return (T) this;
        }

        public T assertSubmitEnabled() {
            await().until(() -> {
                var found = driver.findElements(cssSelector("#authModal button[type=\"submit\"]"));
                return found.size() == 1 && found.get(0).isEnabled();
            });
            return (T) this;
        }

        public T submit() {
            var btn = wait.until(elementToBeClickable(cssSelector("#authModal button[type=\"submit\"]")));
            reliableClick(btn);
            return (T) this;
        }

        public T usePassword(String password) {
            useFieldValue("input[name=\"password\"]", password);
            return (T) this;
        }

        public T waitForReady() {
            App.this.waitForReady();
            return (T) this;
        }
    }

    public class BlogManagePage extends Page<BlogManagePage> {
        private BlogManagePage() {}

        public BlogManagePage assertBlogCount(int expected) {
            var rows = driver.findElements(cssSelector(".pages-manage__row"));
            assertThat(rows).hasSize(expected);
            return this;
        }

        public BlogManagePage assertBlogListed(String title, String publicUrl) {
            var rows = wait.until(visibilityOfAllElementsLocatedBy(cssSelector(".pages-manage__row")));
            assertThat(rows).anySatisfy(rowElm -> {
                assertThat(rowElm.findElement(cssSelector(".pages-manage__row-title")).getText()).isEqualTo(title);
                assertThat(rowElm.findElement(cssSelector(".pages-manage__row-url")).getAttribute("href")).endsWith(publicUrl);
            });
            return this;
        }

        public BlogManagePage assertDeactivateButtonNotPresent() {
            assertThat(driver.findElements(cssSelector(".pages-form__actions .btn--danger"))).isEmpty();
            return this;
        }

        public BlogManagePage assertDeactivateNotAvailableOnList(String title) {
            var row = findRowByTitle(title);
            assertThat(row.findElements(cssSelector(".btn--danger"))).isEmpty();
            return this;
        }

        public BlogManagePage assertEditNotAvailableOnList(String title) {
            var row = findRowByTitle(title);
            assertThat(row.findElements(By.linkText("Edit"))).isEmpty();
            return this;
        }

        public BlogManagePage assertFieldError(String... errorMessages) {
            var errors = driver.findElements(cssSelector(".form-group .error-message.visible"));
            assertThat(errors).hasSize(errorMessages.length)
                              .allMatch(WebElement::isDisplayed)
                              .extracting(WebElement::getText)
                              .containsExactlyInAnyOrder(errorMessages);
            return this;
        }

        public BlogManagePage assertManagePageNotLoaded() {
            assertThat(driver.findElements(cssSelector(".pages-manage__title"))).isEmpty();
            return this;
        }

        public BlogManagePage assertNameEmpty() {
            var input = wait.until(visibilityOfElementLocated(cssSelector("#blogName")));
            assertThat(input.getAttribute("value")).isBlank();
            return this;
        }

        public BlogManagePage assertSlugEmpty() {
            var input = wait.until(visibilityOfElementLocated(cssSelector("#blogSlug")));
            assertThat(input.getAttribute("value")).isBlank();
            return this;
        }

        public BlogManagePage assertSubmitDisabled() {
            var btn = wait.until(visibilityOfElementLocated(cssSelector("button[type='submit']")));
            await().until(() -> !btn.isEnabled());
            return this;
        }

        public BlogManagePage assertSubmitEnabled() {
            var btn = wait.until(visibilityOfElementLocated(cssSelector("button[type='submit']")));
            await().until(btn::isEnabled);
            return this;
        }

        public BlogManagePage assertTitle(String expected) {
            var title = wait.until(visibilityOfElementLocated(cssSelector(".hub-panel__title, .pages-manage__title")));
            assertThat(title.getText()).isEqualTo(expected);
            return this;
        }

        public BlogManagePage assertToastSuccess(String message) {
            waitForToastMessage(message);
            return this;
        }

        public BlogManagePage clearSlug() {
            var input = wait.until(visibilityOfElementLocated(cssSelector("#blogSlug")));
            input.clear();
            input.sendKeys(" ");
            input.sendKeys(Keys.BACK_SPACE);
            return this;
        }

        public BlogManagePage clickDeactivate(String title) {
            var row = findRowByTitle(title);
            reliableClick(row.findElement(cssSelector(".btn--danger")));
            wait.until(d -> {
                try {
                    driver.switchTo().alert().accept();
                    return true;
                } catch (Exception _) {
                    return false;
                }
            });
            waitForReady();
            return this;
        }

        public BlogManagePage clickEdit(String title) {
            var row = findRowByTitle(title);
            reliableClick(row.findElement(By.linkText("Edit")));
            wait.until(visibilityOfElementLocated(cssSelector("#blogName")));
            return this;
        }

        public BlogManagePage clickNewBlog() {
            reliableClick(wait.until(visibilityOfElementLocated(cssSelector("a[data-hx-get*='blogs/new']"))));
            wait.until(visibilityOfElementLocated(cssSelector("#blogName")));
            waitForReady();
            return this;
        }

        public BlogManagePage clickSettings(String title) {
            var row = findRowByTitle(title);
            reliableClick(row.findElement(By.linkText("Settings")));
            wait.until(visibilityOfElementLocated(cssSelector("#blogDescription")));
            waitForReady();
            return this;
        }

        public BlogManagePage fillDescription(String description) {
            var textarea = wait.until(visibilityOfElementLocated(cssSelector("#blogDescription")));
            textarea.clear();
            textarea.sendKeys(description);
            return this;
        }

        public BlogManagePage fillName(String name) {
            var input = wait.until(visibilityOfElementLocated(cssSelector("#blogName")));
            input.clear();
            input.sendKeys(name);
            return this;
        }

        public BlogManagePage fillSlug(String slug) {
            var input = wait.until(visibilityOfElementLocated(cssSelector("#blogSlug")));
            input.clear();
            input.sendKeys(slug);
            return this;
        }

        private WebElement findRowByTitle(String title) {
            var rows = wait.until(visibilityOfAllElementsLocatedBy(cssSelector(".pages-manage__row")));
            return rows.stream()
                       .filter(row -> row.findElement(cssSelector(".pages-manage__row-title")).getText().equals(title))
                       .findFirst()
                       .orElseThrow();
        }

        public BlogPage openPublicBlog(String publicPath) {
            _goTo(publicPath);
            return new BlogPage();
        }

        public BlogManagePage submit() {
            var submitBtn = wait.until(visibilityOfElementLocated(cssSelector("button[type='submit']")));
            await().until(submitBtn::isEnabled);
            reliableClick(submitBtn);
            waitForReady();
            return this;
        }
    }

    public class BlogPage extends Page<BlogPage> {
        private BlogPage() {}

        public BlogPage assertBlogName(String userName) {
            var blogHeader = wait.until(visibilityOfElementLocated(cssSelector(".user-blog__name")));
            assertThat(blogHeader.getText()).isEqualToIgnoringCase(userName);
            return this;
        }

        @Override
        public BlogPage assertLoadMoreIsNotVisible() {
            _assertLoadMoreVisibility(false);
            return this;
        }

        @Override
        public BlogPage assertLoadMoreIsVisible() {
            _assertLoadMoreVisibility(true);
            return this;
        }

        public BlogPage assertNoFeaturedCard() {
            App.this.assertNoFeaturedCard();
            return this;
        }

        public BlogPage assertNotPostMessage() {
            App.this.assertNotPostMessage();
            return this;
        }

        public BlogPage assertNumberOfPosts(int numberOfPosts) {
            _assertNumberOfPosts(numberOfPosts);
            return this;
        }

        public BlogPage assertPostTitle(String title) {
            var postTitle = wait.until(visibilityOfElementLocated(cssSelector(".article-page__title")));
            assertThat(postTitle.getText()).isNotBlank().isEqualTo(title);
            return this;
        }

        public BlogPage clickFirstPostTitle() {
            _clickFirstPostTitle();
            return this;
        }

        public Featured featuredCard() {
            return App.this.featuredCard();
        }

        @Override
        public BlogPage loadMore() {
            _loadMore();
            return this;
        }
    }

    public class CommentManagePage extends Page<CommentManagePage> {
        private CommentManagePage() {}

        public CommentManagePage assertCommentNotListed(String bodySnippet) {
            var rows = driver.findElements(cssSelector(".pages-manage__row"));
            assertThat(rows).noneMatch(row -> row.getText().contains(bodySnippet));
            return this;
        }

        public CommentManagePage assertEmptyState() {
            wait.until(visibilityOfElementLocated(cssSelector(".pages-manage__empty")));
            return this;
        }

        public CommentManagePage assertManagePageNotLoaded() {
            assertThat(driver.findElements(cssSelector(".pages-manage__title"))).isEmpty();
            return this;
        }

        public CommentManagePage assertPendingComment(String bodySnippet) {
            var main = wait.until(visibilityOfElementLocated(By.tagName("main")));
            assertThat(main.getText()).contains(bodySnippet);
            return this;
        }

        public CommentManagePage assertTitle(String expected) {
            var title = wait.until(visibilityOfElementLocated(cssSelector(".hub-panel__title")));
            assertThat(title.getText()).isEqualTo(expected);
            return this;
        }

        public CommentManagePage assertToastSuccess(String message) {
            waitForToastMessage(message);
            return this;
        }

        public CommentManagePage clickApprove(String bodySnippet) {
            var row = findRowByBody(bodySnippet);
            reliableClick(row.findElement(By.xpath(".//button[contains(text(),'Approve')]")));
            waitForReady();
            return this;
        }

        public CommentManagePage clickReject(String bodySnippet) {
            var row = findRowByBody(bodySnippet);
            reliableClick(row.findElement(By.xpath(".//button[contains(text(),'Reject')]")));
            waitForReady();
            return this;
        }

        private WebElement findRowByBody(String bodySnippet) {
            var rows = wait.until(visibilityOfAllElementsLocatedBy(cssSelector(".pages-manage__row")));
            return rows.stream()
                       .filter(row -> row.getText().contains(bodySnippet))
                       .findFirst()
                       .orElseThrow();
        }
    }

    public class CustomPageManagePage extends Page<CustomPageManagePage> {
        private CustomPageManagePage() {}

        public CustomPageManagePage assertManagePageNotLoaded() {
            assertThat(driver.findElements(cssSelector(".pages-manage__title"))).isEmpty();
            return this;
        }

        public CustomPageManagePage assertPageCount(int expected) {
            var rows = driver.findElements(cssSelector(".pages-manage__row"));
            assertThat(rows).hasSize(expected);
            return this;
        }

        public CustomPageManagePage assertPageListed(String title, String publicUrl) {
            var rows = wait.until(visibilityOfAllElementsLocatedBy(cssSelector(".pages-manage__row")));
            assertThat(rows).anySatisfy(rowElm -> {
                assertThat(rowElm.findElement(cssSelector(".pages-manage__row-title")).getText()).isEqualTo(title);
                assertThat(rowElm.findElement(cssSelector(".pages-manage__row-url")).getAttribute("href")).endsWith(publicUrl);
            });
            return this;
        }

        public CustomPageManagePage assertTitle(String expected) {
            var title = wait.until(visibilityOfElementLocated(cssSelector(".hub-panel__title, .pages-manage__title")));
            assertThat(title.getText()).isEqualTo(expected);
            return this;
        }

        public CustomPageManagePage assertToastSuccess(String message) {
            waitForToastMessage(message);
            return this;
        }

        public CustomPageManagePage clickDelete(String title) {
            var row = findRowByTitle(title);
            reliableClick(row.findElement(cssSelector(".btn--danger")));
            wait.until(d -> {
                try {
                    driver.switchTo().alert().accept();
                    return true;
                } catch (Exception _) {
                    return false;
                }
            });
            waitForReady();
            return this;
        }

        public CustomPageManagePage clickEdit(String title) {
            var row = findRowByTitle(title);
            reliableClick(row.findElement(By.linkText("Edit")));
            wait.until(visibilityOfElementLocated(cssSelector("input[name='title']")));
            return this;
        }

        public CustomPageManagePage clickNewPage() {
            reliableClick(wait.until(visibilityOfElementLocated(cssSelector("a[data-hx-get='/pages/new']"))));
            waitForReady();
            return this;
        }

        public CustomPageManagePage fillContent(String content) {
            var textarea = wait.until(visibilityOfElementLocated(cssSelector("textarea[name='content']")));
            textarea.clear();
            textarea.sendKeys(content);
            return this;
        }

        public CustomPageManagePage fillSection(String section) {
            var input = wait.until(visibilityOfElementLocated(cssSelector("input[name='section']")));
            input.clear();
            input.sendKeys(section);
            return this;
        }

        public CustomPageManagePage fillSlug(String slug) {
            var input = wait.until(visibilityOfElementLocated(cssSelector("input[name='slug']")));
            input.clear();
            input.sendKeys(slug);
            return this;
        }

        public CustomPageManagePage fillTitle(String title) {
            var input = wait.until(visibilityOfElementLocated(cssSelector("input[name='title']")));
            input.clear();
            input.sendKeys(title);
            return this;
        }

        private WebElement findRowByTitle(String title) {
            var rows = wait.until(visibilityOfAllElementsLocatedBy(cssSelector(".pages-manage__row")));
            return rows.stream()
                       .filter(row -> row.findElement(cssSelector(".pages-manage__row-title")).getText().equals(title))
                       .findFirst()
                       .orElseThrow();
        }

        public App openPublicPage(String publicPath) {
            _goTo(publicPath);
            return App.this;
        }

        public CustomPageManagePage selectApplicationScope() {
            var select = wait.until(visibilityOfElementLocated(cssSelector("#pageScope")));
            new org.openqa.selenium.support.ui.Select(select).selectByValue("application");
            return this;
        }

        public CustomPageManagePage submit() {
            var submitBtn = wait.until(elementToBeClickable(cssSelector("main form.pages-form button[type='submit']")));
            reliableClick(submitBtn);
            waitForReady();
            return this;
        }
    }

    public class DashboardPage extends Page<DashboardPage> {
        private DashboardPage() {}

        public DashboardPage assertAnalyticsLoaded() {
            wait.until(visibilityOfElementLocated(cssSelector("#dashboardAnalytics .dashboard-chart")));
            return this;
        }

        public DashboardPage assertCompareLegendVisible() {
            wait.until(visibilityOfElementLocated(cssSelector(".dashboard-chart__legend")));
            return this;
        }

        public DashboardPage assertDraftsStatCount(int expected) {
            var stat = wait.until(visibilityOfElementLocated(cssSelector(".stat-card:first-child .stat-card__value")));
            assertThat(stat.getText()).isEqualTo(Integer.toString(expected));
            return this;
        }

        public DashboardPage assertEmptyDraftsMessage() {
            var section = wait.until(visibilityOfElementLocated(cssSelector(".recent-section:first-child")));
            var msg = section.findElement(cssSelector(".recent-section__empty"));
            assertThat(msg.getText()).contains("No drafts yet");
            return this;
        }

        public DashboardPage assertEmptyPublishedMessage() {
            var section = wait.until(visibilityOfElementLocated(cssSelector(".recent-section:last-child")));
            var msg = section.findElement(cssSelector(".recent-section__empty"));
            assertThat(msg.getText()).contains("No published posts yet");
            return this;
        }

        public DashboardPage assertMonthLabel(String expectedLabel) {
            var label = wait.until(visibilityOfElementLocated(cssSelector(".dashboard-analytics__month-label")));
            assertThat(label.getText()).isEqualTo(expectedLabel);
            return this;
        }

        public DashboardPage assertNewFollowersSummary(long newThisMonth, long total) {
            var summary = wait.until(visibilityOfElementLocated(cssSelector("#dashboardFollowersTitle + .dashboard-chart__summary")));
            assertThat(summary.getText()).contains("+" + newThisMonth + " new this month");
            assertThat(summary.getText()).contains(total + " followers total");
            return this;
        }

        public DashboardPage assertPublishedStatCount(int expected) {
            var stat = wait.until(visibilityOfElementLocated(cssSelector(".stat-card:last-child .stat-card__value")));
            assertThat(stat.getText()).isEqualTo(Integer.toString(expected));
            return this;
        }

        public DashboardPage assertRecentDraftTitle(int index, String expectedTitle) {
            var items = driver.findElements(cssSelector(".recent-section:first-child .recent-list__item"));
            assertThat(items).hasSizeGreaterThan(index);
            var title = items.get(index).findElement(cssSelector(".recent-list__title"));
            assertThat(title.getText()).contains(expectedTitle);
            return this;
        }

        public DashboardPage assertRecentDraftsCount(int expected) {
            var items = driver.findElements(cssSelector(".recent-section:first-child .recent-list__item"));
            assertThat(items).hasSize(expected);
            return this;
        }

        public DashboardPage assertRecentPublishedCount(int expected) {
            var items = driver.findElements(cssSelector(".recent-section:last-child .recent-list__item"));
            assertThat(items).hasSize(expected);
            return this;
        }

        public DashboardPage assertRecentPublishedTitle(int index, String expectedTitle) {
            var items = driver.findElements(cssSelector(".recent-section:last-child .recent-list__item"));
            assertThat(items).hasSizeGreaterThan(index);
            var title = items.get(index).findElement(cssSelector(".recent-list__title"));
            assertThat(title.getText()).contains(expectedTitle);
            return this;
        }

        public DashboardPage assertTitle(String expected) {
            var title = wait.until(visibilityOfElementLocated(cssSelector(".hub-panel__title")));
            assertThat(title.getText()).isEqualTo(expected);
            return this;
        }

        public DashboardPage assertViewCountForRecentPublished(int index, int expectedViews) {
            var items = driver.findElements(cssSelector(".recent-section:last-child .recent-list__item"));
            assertThat(items).hasSizeGreaterThan(index);
            var meta = items.get(index).findElement(cssSelector(".recent-list__meta"));
            var viewsSpan = meta.findElements(cssSelector("span")).stream()
                                .filter(span -> span.getText().contains("views"))
                                .findFirst()
                                .orElseThrow();
            assertThat(viewsSpan.getText()).contains(expectedViews + " views");
            return this;
        }

        public DashboardPage assertViewsSummary(long expectedViews) {
            var summary = wait.until(visibilityOfElementLocated(cssSelector("#dashboardViewsTitle + .dashboard-chart__summary")));
            assertThat(summary.getText()).contains(expectedViews + " views this month");
            return this;
        }

        public DashboardPage clickPreviousMonth() {
            var links = wait.until(visibilityOfAllElementsLocatedBy(cssSelector(".dashboard-analytics__month-link")));
            reliableClick(links.getFirst());
            waitForReady();
            wait.until(visibilityOfElementLocated(cssSelector("#dashboardAnalytics .dashboard-chart")));
            return this;
        }

        public WritePage clickRecentDraft(int index) {
            var items = wait.until(visibilityOfAllElementsLocatedBy(cssSelector(".recent-section:first-child .recent-list__item")));
            assertThat(items).hasSizeGreaterThan(index);
            reliableClick(items.get(index).findElement(cssSelector(".recent-list__title")));
            waitForReady();
            return new WritePage();
        }

        public PostPage clickRecentPublished(int index) {
            var items = wait.until(visibilityOfAllElementsLocatedBy(cssSelector(".recent-section:last-child .recent-list__item")));
            assertThat(items).hasSizeGreaterThan(index);
            reliableClick(items.get(index).findElement(cssSelector(".recent-list__title")));
            waitForReady();
            return new PostPage();
        }

        public LibraryPage clickViewAllDrafts() {
            var link = wait.until(visibilityOfElementLocated(cssSelector(".stat-card:first-child .stat-card__link")));
            reliableClick(link);
            waitForReady();
            // After click, we expect the library page with drafts tab active
            return new LibraryPage();
        }

        public LibraryPage clickViewAllPublished() {
            var link = wait.until(visibilityOfElementLocated(cssSelector(".stat-card:last-child .stat-card__link")));
            reliableClick(link);
            waitForReady();
            return new LibraryPage();
        }

        public WritePage clickWriteNewStory() {
            var btn = wait.until(visibilityOfElementLocated(cssSelector(".dashboard-action .btn")));
            reliableClick(btn);
            waitForReady();
            return new WritePage();
        }

        public DashboardPage enableCompareViews() {
            var link = wait.until(elementToBeClickable(cssSelector("a.dashboard-analytics__compare-link")));
            if (link.getText().contains("Compare with previous month")) {
                reliableClick(link);
                waitForReady();
                wait.until(visibilityOfElementLocated(cssSelector(".dashboard-chart__legend")));
            }
            return this;
        }

        public DashboardPage selectBlog(String blogName) {
            var selectElement = wait.until(visibilityOfElementLocated(By.id("dashboardBlogSelect")));
            new Select(selectElement).selectByVisibleText(blogName);
            waitForReady();
            wait.until(visibilityOfElementLocated(cssSelector("#dashboardAnalytics .dashboard-chart")));
            return this;
        }
    }

    public class Featured {
        private final WebElement elm;

        private Featured(WebElement elm) {
            this.elm = elm;
        }

        public BlogPage accessAuthorBlog() {
            var authorLink = elm.findElement(cssSelector(".article-meta__author"));
            // Actually the link's href or data-hx-get attribute contains the username
            var hxGet = driver.findElement(RelativeLocator.with(By.cssSelector("[data-hx-get"))
                                                          .near(authorLink))
                              .getAttribute("data-hx-get");
            assertThat(hxGet).isNotNull();
            //
            reliableClick(authorLink);
            wait.until(d -> d.getCurrentUrl().contains(hxGet));
            return new BlogPage();
        }

        public BlogPage accessPost() {
            var featuredTitle = elm.findElement(className("featured__title"));
            reliableClick(featuredTitle);
            return new BlogPage();
        }

        public Featured assertAuthorName(String authorName) {
            var authorLink = elm.findElement(cssSelector(".article-meta__author"));
            assertThat(authorLink.getText()).isEqualTo(authorName);
            return this;
        }

        public Featured assertLink() {
            var featuredLink = elm.findElement(By.tagName("a"));
            assertThat(featuredLink.getAttribute("data-hx-get")).isNotNull();
            return this;
        }

        public Featured assertNoCoverImage() {
            assertThat(elm.findElements(cssSelector(".featured__image"))).isEmpty();
            return this;
        }

        public Featured assertNoCoverLayout() {
            var grid = elm.findElement(className("featured__grid"));
            assertThat(grid.getAttribute("class")).contains("featured__grid--no-cover");
            return this;
        }

        public Featured assertTitle() {
            var featuredTitle = elm.findElement(className("featured__title"));
            assertThat(featuredTitle.getText()).isNotBlank();
            return this;
        }

        public Featured assertTitle(String title) {
            var featuredTitle = elm.findElement(className("featured__title"));
            assertThat(featuredTitle.getText()).isEqualTo(title);
            return this;
        }
    }

    public class GitSyncHistoryPage extends Page<GitSyncHistoryPage> {

        private GitSyncHistoryPage() {}

        public GitSyncHistoryPage assertDetailShows(String label) {
            var panel = wait.until(visibilityOfElementLocated(cssSelector(".git-sync-history__summary-panel")));
            assertThat(panel.getText()).contains(label);
            return this;
        }

        public GitSyncHistoryPage assertGitSyncHistoryTitle() {
            var title = wait.until(visibilityOfElementLocated(cssSelector(".pages-manage__title")));
            assertThat(title.getText()).isEqualTo("Git sync history");
            return this;
        }

        public GitSyncHistoryPage assertRunListed(String summaryFragment) {
            wait.until(visibilityOfElementLocated(cssSelector(".git-sync-history")));
            assertThat(driver.getPageSource()).contains(summaryFragment);
            return this;
        }
    }

    public class ImageControlPage extends Page<ImageControlPage> {

        private ImageControlPage() {}

        public ImageControlPage assertImageControlTitle() {
            var title = wait.until(visibilityOfElementLocated(cssSelector(".pages-manage__title")));
            assertThat(title.getText()).isEqualTo("Images");
            return this;
        }

        public ImageControlPage assertImageListed(String filename) {
            var rows = wait.until(visibilityOfAllElementsLocatedBy(cssSelector(".image-control__row")));
            assertThat(rows).anyMatch(row -> row.getText().contains(filename));
            return this;
        }

        public ImageControlPage assertImageUsage(String postTitle) {
            var usage = wait.until(visibilityOfElementLocated(cssSelector(".image-control__usage-list")));
            assertThat(usage.getText()).contains(postTitle);
            return this;
        }
    }

    public class LibraryPage extends Page<LibraryPage> {
        private LibraryPage() {}

        public LibraryPage assertDraftNotPresent(String title) {
            var drafts = driver.findElements(By.cssSelector(".drafts-list .draft-card__title"));
            assertThat(drafts).extracting(WebElement::getText)
                              .noneMatch(text -> text.equals(title));
            return this;
        }

        public LibraryPage assertDraftPresent(String title) {
            assertThat(driver.getPageSource()).contains(title);
            return this;
        }

        public LibraryPage deleteFirstDraft() {
            var deleteBtn = wait.until(visibilityOfElementLocated(cssSelector(".draft-card .btn--danger")));
            reliableClick(deleteBtn);
            driver.switchTo().alert().accept();
            wait.until(d -> driver.findElements(cssSelector(".draft-card")).isEmpty());
            return this;
        }

        public LibraryPage switchTab(String tab) { // "drafts" or "published"
            var tabButton = wait.until(elementToBeClickable(cssSelector(".library-tab[data-tab='" + tab + "']")));
            reliableClick(tabButton);
            wait.until(visibilityOfElementLocated(cssSelector(".library-tab.library-tab--active[data-tab='" + tab + "']")));
            waitForReady();
            return this;
        }
    }

    public final class Login extends AccessModal<Login> {
        private Login() {}

        public Login closeModal() {
            var closeBtn = driver.findElement(By.cssSelector("#authModal .modal__close"));
            reliableClick(closeBtn);
            return this;
        }

        public Signup switchToSignup() {
            reliableClick(wait.until(visibilityOfElementLocated(cssSelector(".auth-form__switch a"))));
            return new Signup();
        }

        public Login useLogin(String login) {
            useFieldValue("input[name=\"login\"]", login);
            return this;
        }
    }

    public abstract class Page<T extends Page<T>> {
        private Page() {}

        public App assertAccessButtonIsDisplayed() {
            App.this.assertAccessButtonIsDisplayed();
            return App.this;
        }

        public App assertErrorPage(Status status) {
            wait.until(visibilityOfElementLocated(cssSelector(".error-page")));
            var errorCode = driver.findElement(cssSelector(".error-code"));
            assertThat(errorCode.getText()).contains(Integer.toString(status.getStatusCode()));
            return App.this;
        }

        public T assertLinks(PagePlacement placement, String... links) {
            _assertLinks(placement, links);
            return (T) this;
        }

        public T assertLoadMoreIsNotVisible() {
            _assertLoadMoreVisibility(false);
            return (T) this;
        }

        public T assertLoadMoreIsVisible() {
            _assertLoadMoreVisibility(true);
            return (T) this;
        }

        public T assertManagePaginationSummary(long total, int page, int totalPages) {
            var summary = wait.until(visibilityOfElementLocated(cssSelector(".manage-pagination__summary")))
                              .getText();
            var itemLabel = total == 1 ? "1 item" : "%d items".formatted(total);
            assertThat(summary).contains(itemLabel);
            assertThat(summary).contains("Page %d of %d".formatted(page, totalPages));
            return (T) this;
        }

        public T assertManagePaginationVisible() {
            assertThat(driver.findElements(cssSelector(".manage-pagination"))).isNotEmpty();
            return (T) this;
        }

        public T assertPageTitleContains(String... titles) {
            assertThat(driver.getTitle()).contains(titles);
            return (T) this;
        }

        public T assertUrl(String url) {
            App.this.assertUrl(url);
            return (T) this;
        }

        public T assertUrlContains(String urlFragment) {
            wait.until(urlContains(urlFragment));
            return (T) this;
        }

        public T assertUrlEquals(String url) {
            assertThat(driver.getCurrentUrl()).isEqualTo(rootUri + url);
            return (T) this;
        }

        public App click(PagePlacement placement, String link) {
            _click(placement, link);
            return App.this;
        }

        public T goToManageNextPage() {
            var next = wait.until(elementToBeClickable(
                                                       cssSelector(".manage-pagination__controls a.manage-pagination__link:last-child")));
            assertThat(next.getText()).isEqualTo("Next");
            reliableClick(next);
            waitForReady();
            return (T) this;
        }

        public App home() {
            var homeBtn = wait.until(visibilityOfElementLocated(By.cssSelector(".logo a")));
            reliableClick(homeBtn);
            waitForReady();
            return App.this;
        }

        public T loadMore() {
            _loadMore();
            return (T) this;
        }

        public App logout() {
            _logout();
            return App.this;
        }

        public T waitForReady() {
            App.this.waitForReady();
            return (T) this;
        }

        protected void waitForToastMessage(String message) {
            await().atMost(Duration.ofSeconds(15))
                   .pollInterval(Duration.ofMillis(100))
                   .until(() -> Boolean.TRUE.equals(((JavascriptExecutor) driver).executeScript("""
                                                                                                const toast = document.getElementById('toast');
                                                                                                if (!toast || !toast.classList.contains('toast--visible')) return false;
                                                                                                return toast.textContent.includes(arguments[0]);
                                                                                                """,
                                                                                                message)));
        }

    }

    public class PasswordRecoveryPage extends Page<PasswordRecoveryPage> {
        private PasswordRecoveryPage() {}

        public PasswordRecoveryPage assertSuccessMessage(String expectedSubstring) {
            var successMsg = wait.until(visibilityOfElementLocated(cssSelector("#recoveryMessage .success-message")));
            assertThat(successMsg.getText()).contains(expectedSubstring);
            return this;
        }

        public PasswordRecoveryPage fillEmail(String email) {
            var input = wait.until(visibilityOfElementLocated(cssSelector("input[name='email']")));
            input.clear();
            input.sendKeys(email);
            return this;
        }

        public PasswordRecoveryPage submit() {
            reliableClick(wait.until(visibilityOfElementLocated(cssSelector("button[type='submit']"))));
            waitForReady();
            return this;
        }
    }

    public class PostPage extends Page<PostPage> {
        private static final String FOLLOW_BUTTON_SELECTOR = ".blog-audience button[hx-post*='/follow']";

        private PostPage() {}

        public PostPage assertChangeDiffVisible() {
            var modal = driver.findElement(By.id("postHistoryModal"));
            wait.until(d -> !modal.findElements(cssSelector(".post-history__diff-ins")).isEmpty());
            return this;
        }

        public PostPage assertChangeHistoryModalTitle() {
            var modal = wait.until(visibilityOfElementLocated(By.id("postHistoryModal")));
            assertThat(modal.findElement(cssSelector(".modal__title")).getText()).isEqualTo("Change history");
            return this;
        }

        public PostPage assertCommentsFormVisible() {
            await().atMost(Duration.ofSeconds(15)).until(() -> {
                var forms = driver.findElements(cssSelector("#comments .comment-form"));
                return forms.size() == 1 && forms.get(0).isDisplayed();
            });
            return this;
        }

        public PostPage assertCommentsSignInGateVisible() {
            await().atMost(Duration.ofSeconds(15)).until(() -> {
                var hints = driver.findElements(cssSelector("#comments .comments-section__login-hint"));
                return hints.size() == 1
                        && hints.get(0).isDisplayed()
                        && hints.get(0).getText().contains("Sign in");
            });
            return this;
        }

        public PostPage assertCoverImagePresent() {
            var coverImage = wait.until(visibilityOfElementLocated(cssSelector(".article-page__cover-image")));
            assertThat(coverImage.isDisplayed()).isTrue();
            return this;
        }

        public PostPage assertFeaturedButtonIsNotPresent() {
            var elements = driver.findElements(By.cssSelector("#post-featured-toggle"));
            assertThat(elements).isEmpty();
            return this;
        }

        public PostPage assertFeaturedButtonIsPresent() {
            var elements = driver.findElements(By.cssSelector("#post-featured-toggle"));
            assertThat(elements).hasSize(1);
            var postFeaturedButton = elements.get(0);
            assertThat(postFeaturedButton.isDisplayed()).isTrue();
            return this;
        }

        public PostPage assertFollowButtonIsAuthenticated() {
            await().atMost(Duration.ofSeconds(15)).until(() -> {
                var buttons = driver.findElements(cssSelector(FOLLOW_BUTTON_SELECTOR));
                return buttons.size() == 1
                        && buttons.get(0).isDisplayed();
            });
            return this;
        }

        public PostPage assertFollowButtonOpensLoginModal() {
            var follow = wait.until(visibilityOfElementLocated(
                                                               cssSelector(".blog-audience button[hx-get*='/auth/modal']")));
            assertThat(follow.getAttribute("hx-get")).contains("/auth/modal");
            return this;
        }

        public PostPage assertPostTitle(String title) {
            var postTitle = wait.until(visibilityOfElementLocated(cssSelector(".article-page__title")));
            assertThat(postTitle.getText()).isNotBlank().isEqualTo(title);
            return this;
        }

        public PostPage assertSerieNavCurrentPart(String title) {
            var nav = wait.until(visibilityOfElementLocated(cssSelector(".post-serie-nav")));
            var current = nav.findElement(cssSelector(".post-serie-nav__item--current"));
            assertThat(current.getText()).isEqualTo(title);
            assertThat(current.findElements(By.tagName("a"))).isEmpty();
            return this;
        }

        public PostPage assertSerieNavLinkedPart(String title) {
            var nav = wait.until(visibilityOfElementLocated(cssSelector(".post-serie-nav")));
            var link = nav.findElements(cssSelector(".post-serie-nav__post-link")).stream()
                          .filter(a -> title.equals(a.getText()))
                          .findFirst()
                          .orElseThrow(() -> new AssertionError("No linked serie part titled: " + title));
            assertThat(link.getAttribute("href")).isNull();
            assertThat(link.getAttribute("data-hx-get")).isNotBlank();
            return this;
        }

        public PostPage assertSerieNavListsPart(String title) {
            var nav = wait.until(visibilityOfElementLocated(cssSelector(".post-serie-nav")));
            assertThat(nav.getText()).contains(title);
            return this;
        }

        public PostPage assertSerieNavPartCount(int parts) {
            var nav = wait.until(visibilityOfElementLocated(cssSelector(".post-serie-nav")));
            var partCount = nav.findElement(cssSelector(".post-serie-nav__part-count"));
            assertThat(partCount.getText()).isEqualTo("Series of " + parts + " parts");
            return this;
        }

        public PostPage assertSerieNavPartListedBefore(String earlierTitle, String laterTitle) {
            var nav = wait.until(visibilityOfElementLocated(cssSelector(".post-serie-nav")));
            String text = nav.getText();
            assertThat(text.indexOf(earlierTitle)).isLessThan(text.indexOf(laterTitle));
            return this;
        }

        public PostPage assertSerieNavVisible(String seriesTitle) {
            var nav = wait.until(visibilityOfElementLocated(cssSelector(".post-serie-nav")));
            var link = nav.findElement(cssSelector(".post-serie-nav__serie-link"));
            assertThat(link.getText()).isEqualTo(seriesTitle);
            return this;
        }

        public PostPage assertVersionInMetadata(int version) {
            var trigger = wait.until(visibilityOfElementLocated(cssSelector(".article-page__version")));
            assertThat(trigger.getText()).containsIgnoringCase("version " + version);
            assertThat(trigger.getText()).containsIgnoringCase("current");
            return this;
        }

        public PostPage clickFollowButton() {
            var follow = wait.until(elementToBeClickable(cssSelector(FOLLOW_BUTTON_SELECTOR)));
            reliableClick(follow);
            waitForReady();
            await().atMost(Duration.ofSeconds(15)).until(() -> {
                var buttons = driver.findElements(cssSelector(FOLLOW_BUTTON_SELECTOR));
                return !buttons.isEmpty() && "Following".equals(buttons.get(0).getText().trim());
            });
            return this;
        }

        public PostPage closeChangeHistoryModal() {
            var modal = wait.until(visibilityOfElementLocated(By.id("postHistoryModal")));
            var closeBtn = modal.findElement(cssSelector(".modal__close"));
            reliableClick(closeBtn);
            wait.until(invisibilityOfElementLocated(By.id("postHistoryModal")));
            return this;
        }

        public PostPage expandFirstChangeDetails() {
            var modal = wait.until(visibilityOfElementLocated(By.id("postHistoryModal")));
            var details = modal.findElement(cssSelector(".post-history__details"));
            if (details.getAttribute("open") == null) {
                reliableClick(details.findElement(cssSelector("summary")));
            }
            return this;
        }

        public PostPage openChangeHistoryModal() {
            var trigger = wait.until(elementToBeClickable(cssSelector(".article-page__version")));
            reliableClick(trigger);
            wait.until(visibilityOfElementLocated(By.id("postHistoryModal")));
            return this;
        }

        public PostPage toggleFeatured() {
            var postFeaturedButton = driver.findElement(By.cssSelector("#post-featured-toggle"));
            reliableClick(postFeaturedButton);
            return this;
        }
    }

    public class ProfilePage extends Page<ProfilePage> {
        private ProfilePage() {}

        public ProfilePage assertEmailIs(String expected) {
            var emailInput = wait.until(visibilityOfElementLocated(cssSelector("input[name='email']")));
            assertThat(emailInput.getAttribute("value")).isEqualTo(expected);
            return this;
        }

        public ProfilePage assertErrorMessage(String expectedSubstring) {
            var errorMsg =
                    wait.until(visibilityOfElementLocated(cssSelector("#securityMessage .error-message, #appearanceMessage .error-message, #profileMessage .error-message")));
            assertThat(errorMsg.getText()).contains(expectedSubstring);
            return this;
        }

        public ProfilePage assertNameIs(String expected) {
            var nameInput = wait.until(visibilityOfElementLocated(cssSelector("input[name='name']")));
            assertThat(nameInput.getAttribute("value")).isEqualTo(expected);
            return this;
        }

        public ProfilePage assertNotPresent() {
            var form = driver.findElements(cssSelector(".profile-form"));
            assertThat(form).isEmpty();
            return this;
        }

        public ProfilePage assertSuccessMessage(String expectedSubstring) {
            var successMsg =
                    wait.until(visibilityOfElementLocated(cssSelector("#securityMessage .success-message, #appearanceMessage .success-message, #profileMessage .success-message")));
            assertThat(successMsg.getText()).contains(expectedSubstring);
            return this;
        }

        public ProfilePage fillConfirmPassword(String password) {
            var input = wait.until(visibilityOfElementLocated(cssSelector("input[name='confirmPassword']")));
            input.clear();
            input.sendKeys(password);
            return this;
        }

        public ProfilePage fillCurrentPassword(String password) {
            var input = wait.until(visibilityOfElementLocated(cssSelector("input[name='currentPassword']")));
            input.clear();
            input.sendKeys(password);
            return this;
        }

        public ProfilePage fillEmail(String email) {
            var input = wait.until(visibilityOfElementLocated(cssSelector("input[name='email']")));
            input.clear();
            input.sendKeys(email);
            return this;
        }

        public ProfilePage fillName(String name) {
            var input = wait.until(visibilityOfElementLocated(cssSelector("input[name='name']")));
            input.clear();
            input.sendKeys(name);
            return this;
        }

        public ProfilePage fillNewPassword(String password) {
            var input = wait.until(visibilityOfElementLocated(cssSelector("input[name='newPassword']")));
            input.clear();
            input.sendKeys(password);
            return this;
        }

        public ProfilePage refresh() {
            driver.navigate().refresh();
            waitForReady();
            return this;
        }

        public ProfilePage submit() {
            reliableClick(wait.until(visibilityOfElementLocated(cssSelector("button[type='submit']"))));
            waitForReady();
            return this;
        }
    }

    public class ReviewPage extends Page<ReviewPage> {
        private ReviewPage() {}

        public ReviewPage assertNumberOfPosts(int numberOfPosts) {
            var reviewRows = wait.until(visibilityOfAllElementsLocatedBy(By.cssSelector(".review-list .review-row")));
            assertThat(reviewRows).hasSize(numberOfPosts);
            return this;
        }

        public ReviewPage toggleFeatured(Post post) {
            var toggleBtn = wait.until(visibilityOfElementLocated(By.cssSelector("#post-row-%d .btn".formatted(post.getId()))));
            reliableClick(toggleBtn);
            waitForReady();
            return this;
        }
    }

    public class SearchModal {
        private SearchModal() {}

        public SearchModal assertAdvancedLinkExists() {
            var modal = wait.until(visibilityOfElementLocated(By.id("searchModal")));
            var link = modal.findElement(cssSelector(".search-modal__advanced"));
            assertThat(link.isDisplayed()).isTrue();
            return this;
        }

        public SearchModal assertEmptyState() {
            var emptyMsg = wait.until(visibilityOfElementLocated(cssSelector(".search-empty")));
            assertThat(emptyMsg.getText()).contains("No results found");
            return this;
        }

        public SearchModal assertResultContains(String text) {
            var modal = driver.findElement(By.id("searchModal"));
            var results = modal.findElements(cssSelector(".search-result"));
            assertThat(results.stream()
                              .anyMatch(r -> r.getText()
                                              .toLowerCase()
                                              .contains(text.toLowerCase()))).isTrue();
            return this;
        }

        public SearchModal assertResultsDisplayed() {
            var modal = wait.until(visibilityOfElementLocated(By.id("searchModal")));
            wait.until(d -> modal.findElements(cssSelector(".search-result")).size() > 0);
            return this;
        }

        public SearchModal assertUrl(String url) {
            App.this.assertUrl(url);
            return this;
        }

        public SearchModal close() {
            var modal = wait.until(visibilityOfElementLocated(By.id("searchModal")));
            var closeBtn = modal.findElement(cssSelector(".modal__close"));
            reliableClick(closeBtn);
            wait.until(invisibilityOfElementLocated(By.id("searchModal")));
            return this;
        }

        public SearchModal type(String query) {
            var modal = wait.until(visibilityOfElementLocated(By.id("searchModal")));
            var input = modal.findElement(cssSelector("input[name='q']"));
            input.sendKeys(query);
            return this;
        }
    }

    public class SearchPage extends Page<SearchPage> {
        private SearchPage() {}

        public SearchPage assertEmptyState() {
            wait.until(visibilityOfElementLocated(cssSelector(".search-empty")));
            return this;
        }

        public SearchPage assertHeaderContainsCountAndQuery(String query) {
            var header = wait.until(visibilityOfElementLocated(cssSelector(".search-results__header")));
            assertThat(header.getText()).contains("Found").contains(query);
            return this;
        }

        public SearchPage assertResultContains(String text) {
            var results = wait.until(visibilityOfAllElementsLocatedBy(cssSelector(".search-result")));
            assertThat(results.stream().anyMatch(r -> r.getText().toLowerCase().contains(text.toLowerCase()))).isTrue();
            return this;
        }

        public SearchPage assertResultCount(int expectedCount) {
            await().alias("Wait for %d results...".formatted(expectedCount))
                   .until(() -> {
                       var results = wait.until(visibilityOfAllElementsLocatedBy(cssSelector(".search-result")));
                       return results.size() == expectedCount;
                   });
            return this;
        }

        public SearchPage assertResultHasAuthorAndDate() {
            var meta = wait.until(visibilityOfElementLocated(cssSelector(".search-result__meta")));
            var author = meta.findElement(cssSelector(".search-result__author"));
            var date = meta.findElement(cssSelector(".search-result__date"));
            assertThat(author.getText()).isNotBlank();
            assertThat(date.getText()).isNotBlank();
            return this;
        }

        public PostPage clickFirstResult() {
            var firstResultLink = wait.until(visibilityOfElementLocated(cssSelector(".search-result__title a")));
            reliableClick(firstResultLink);
            waitForReady();
            return new PostPage();
        }

        @Override
        public SearchPage loadMore() {
            var loadMoreBtn = wait.until(elementToBeClickable(cssSelector("#search-more-results button")));
            reliableClick(loadMoreBtn);
            waitForReady();
            return this;
        }

        public SearchPage search(String query) {
            var input = wait.until(visibilityOfElementLocated(cssSelector("input[name='q']")));
            input.sendKeys(query);
            var submitBtn = driver.findElement(cssSelector(".search-form__button"));
            reliableClick(submitBtn);
            waitForReady();
            return this;
        }

        public SearchPage submit() {
            var submitBtn = driver.findElement(cssSelector(".search-form__button"));
            reliableClick(submitBtn);
            waitForReady();
            return this;
        }

        public SearchPage type(String query) {
            var input = wait.until(visibilityOfElementLocated(cssSelector("input[name='q']")));
            input.sendKeys(query);
            return this;
        }
    }

    public class SerieBrowsePage extends Page<SerieBrowsePage> {
        private SerieBrowsePage() {}

        public SerieBrowsePage assertListsPostTitle(String title) {
            wait.until(visibilityOfElementLocated(cssSelector(".article-card__title")));
            var main = wait.until(visibilityOfElementLocated(By.tagName("main")));
            assertThat(main.getText()).contains(title);
            return this;
        }

        /**
         * Published order is oldest-first; body text should list part1 before part2.
         */
        public SerieBrowsePage assertPostListedBefore(String earlierTitle, String laterTitle) {
            wait.until(visibilityOfElementLocated(cssSelector(".article-card__title")));
            var main = wait.until(visibilityOfElementLocated(By.tagName("main")));
            String text = main.getText();
            assertThat(text.indexOf(earlierTitle)).isLessThan(text.indexOf(laterTitle));
            return this;
        }
    }

    public final class Signup extends AccessModal<Signup> {
        private Signup() {}

        public Signup useEmail(String email) {
            useFieldValue("input[name=\"email\"]", email);
            return this;
        }

        public Signup useName(String name) {
            useFieldValue("input[name=\"name\"]", name);
            return this;
        }

        public Signup useUsername(String username) {
            useFieldValue("input[name=\"username\"]", username);
            return this;
        }
    }

    public class TagBrowsePage extends Page<TagBrowsePage> {
        private TagBrowsePage() {}

        public TagBrowsePage assertListsPostTitle(String title) {
            wait.until(visibilityOfElementLocated(cssSelector(".article-card__title")));
            var main = wait.until(visibilityOfElementLocated(By.tagName("main")));
            assertThat(main.getText()).contains(title);
            return this;
        }
    }

    public class TagEditPage extends Page<TagEditPage> {
        private TagEditPage() {}

        public TagEditPage fillName(String name) {
            var input = wait.until(visibilityOfElementLocated(cssSelector("#tagDisplayName")));
            input.clear();
            input.sendKeys(name);
            return this;
        }

        public TagManagePage submit() {
            reliableClick(wait.until(elementToBeClickable(cssSelector("#tagEditForm button[type='submit']"))));
            waitForReady();
            return new TagManagePage();
        }
    }

    public class TagManagePage extends Page<TagManagePage> {
        private TagManagePage() {}

        public TagManagePage assertManagePageNotLoaded() {
            assertThat(driver.findElements(cssSelector(".pages-manage__title"))).isEmpty();
            return this;
        }

        public TagManagePage assertTagListed(String name) {
            var rows = wait.until(visibilityOfAllElementsLocatedBy(cssSelector(".pages-manage__row")));
            assertThat(rows).anyMatch(row -> row.findElement(cssSelector(".pages-manage__row-title")).getText().equals(name));
            return this;
        }

        public TagManagePage assertTitle(String expected) {
            var title = wait.until(visibilityOfElementLocated(cssSelector(".hub-panel__title")));
            assertThat(title.getText()).isEqualTo(expected);
            return this;
        }

        public TagEditPage clickEdit(String name) {
            var row = findRowByTitle(name);
            var path = row.findElement(By.linkText("Edit")).getAttribute("data-hx-get");
            _goTo(path);
            wait.until(visibilityOfElementLocated(cssSelector("#tagEditForm")));
            return new TagEditPage();
        }

        private WebElement findRowByTitle(String title) {
            var rows = wait.until(visibilityOfAllElementsLocatedBy(cssSelector(".pages-manage__row")));
            return rows.stream()
                       .filter(row -> row.findElement(cssSelector(".pages-manage__row-title")).getText().equals(title))
                       .findFirst()
                       .orElseThrow();
        }
    }

    public class UserManagePage extends Page<UserManagePage> {
        private UserManagePage() {}

        public UserManagePage assertManagePageNotLoaded() {
            assertThat(driver.findElements(cssSelector(".pages-manage__title"))).isEmpty();
            return this;
        }

        public UserManagePage assertTitle(String expected) {
            var title = wait.until(visibilityOfElementLocated(cssSelector(".hub-panel__title, .pages-manage__title")));
            assertThat(title.getText()).isEqualTo(expected);
            return this;
        }

        public UserManagePage assertToastSuccess(String message) {
            waitForToastMessage(message);
            return this;
        }

        public UserManagePage assertUserListed(String name) {
            var rows = wait.until(visibilityOfAllElementsLocatedBy(cssSelector(".pages-manage__row")));
            assertThat(rows).anyMatch(row -> row.findElement(cssSelector(".pages-manage__row-title")).getText().equals(name));
            return this;
        }

        public UserManagePage clickEdit(String name) {
            var row = findRowByTitle(name);
            reliableClick(row.findElement(By.linkText("Edit")));
            wait.until(visibilityOfElementLocated(cssSelector("#userName")));
            return this;
        }

        public UserManagePage clickNewUser() {
            reliableClick(wait.until(visibilityOfElementLocated(cssSelector("a[data-hx-get='/users/new']"))));
            waitForReady();
            return this;
        }

        public UserManagePage fillEmail(String email) {
            var input = wait.until(visibilityOfElementLocated(cssSelector("#userEmail")));
            input.clear();
            input.sendKeys(email);
            return this;
        }

        public UserManagePage fillName(String name) {
            var input = wait.until(visibilityOfElementLocated(cssSelector("#userName")));
            input.clear();
            input.sendKeys(name);
            return this;
        }

        public UserManagePage fillNewPassword(String password) {
            var input = wait.until(visibilityOfElementLocated(cssSelector("#userNewPassword")));
            input.clear();
            input.sendKeys(password);
            return this;
        }

        public UserManagePage fillPassword(String password) {
            var input = wait.until(visibilityOfElementLocated(cssSelector("#userPassword")));
            input.clear();
            input.sendKeys(password);
            return this;
        }

        public UserManagePage fillUsername(String username) {
            var input = wait.until(visibilityOfElementLocated(cssSelector("#userUsername")));
            input.clear();
            input.sendKeys(username);
            return this;
        }

        private WebElement findRowByTitle(String title) {
            var rows = wait.until(visibilityOfAllElementsLocatedBy(cssSelector(".pages-manage__row")));
            return rows.stream()
                       .filter(row -> row.findElement(cssSelector(".pages-manage__row-title")).getText().equals(title))
                       .findFirst()
                       .orElseThrow();
        }

        public UserManagePage setActive(boolean active) {
            var checkbox = wait.until(visibilityOfElementLocated(cssSelector("input[name='active']")));
            if (checkbox.isSelected() != active) {
                reliableClick(checkbox);
            }
            return this;
        }

        public UserManagePage submit() {
            var submitBtn = wait.until(visibilityOfElementLocated(cssSelector("main form.profile-form button[type='submit']")));
            await().until(submitBtn::isEnabled);
            reliableClick(submitBtn);
            waitForReady();
            return this;
        }
    }

    public class WritePage extends Page<WritePage> {
        private WritePage() {}

        public WritePage assertContent(String content) {
            var textarea = wait.until(visibilityOfElementLocated(cssSelector("#content")));
            assertThat(textarea.getAttribute("value")).isEqualTo(content);
            return this;
        }

        public WritePage assertCoverPreviewNotVisible() {
            assertThat(driver.findElement(By.id("coverPreview")).isDisplayed()).isFalse();
            return this;
        }

        public WritePage assertCoverPreviewVisible() {
            assertThat(driver.findElement(By.id("coverPreview")).isDisplayed()).isTrue();
            return this;
        }

        public WritePage assertTitle(String title) {
            var input = wait.until(visibilityOfElementLocated(cssSelector("#title")));
            assertThat(input.getAttribute("value")).isEqualTo(title);
            return this;
        }

        private void assertToast(String message) {
            waitForToastMessage(message);
        }

        public WritePage assertToastError(String message) {
            assertToast(message);
            return this;
        }

        public WritePage assertToastSuccess(String message) {
            assertToast(message);
            return this;
        }

        public WritePage fillContent(String content) {
            var textarea = wait.until(visibilityOfElementLocated(cssSelector("#content")));
            textarea.clear();
            textarea.sendKeys(content);
            return this;
        }

        public WritePage fillTitle(String title) {
            var input = wait.until(visibilityOfElementLocated(cssSelector("#title")));
            input.clear();
            input.sendKeys(title);
            return this;
        }

        public Long getCurrentDraftId() {
            var url = driver.getCurrentUrl();
            var matcher = Pattern.compile("/write/draft/(\\d+)").matcher(url);
            assertThat(matcher.find()).isTrue();
            return Long.parseLong(matcher.group(1));
        }

        public WritePage publish() {
            var publishBtn = wait.until(visibilityOfElementLocated(By.id("publish")));
            reliableClick(publishBtn);
            waitForToast();
            return this;
        }

        public WritePage removeCover() {
            var removeBtn = wait.until(visibilityOfElementLocated(By.id("removeCoverBtn")));
            reliableClick(removeBtn);
            wait.until(d -> !driver.findElement(By.id("coverPreview")).isDisplayed());
            return this;
        }

        public WritePage saveDraft() {
            var saveBtn = wait.until(visibilityOfElementLocated(By.id("saveDraft")));
            reliableClick(saveBtn);
            waitForToast(); // wait for success/error toast
            return this;
        }

        public WritePage uploadCover(Path imagePath) {
            var coverInput = wait.until(presenceOfElementLocated(By.id("coverInput")));
            coverInput.sendKeys(imagePath.toAbsolutePath().toString());
            wait.until(d -> driver.findElement(By.id("coverPreview")).isDisplayed());
            return this;
        }

        public WritePage waitForToast() {
            await().atMost(Duration.ofSeconds(15))
                   .pollInterval(Duration.ofMillis(100))
                   .until(() -> Boolean.TRUE.equals(((JavascriptExecutor) driver).executeScript("""
                                                                                                const toast = document.getElementById('toast');
                                                                                                return toast != null && toast.classList.contains('toast--visible');
                                                                                                """)));
            return this;
        }
    }

    private final String rootUri;

    private final WebDriver driver;

    private final WebDriverWait wait;

    public App(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
        this.rootUri = TestHTTPResourceManager.getUri();
    }

    private void _assertLinks(PagePlacement placement, String... links) {
        switch (placement) {
            case FOOTER:
                var footerElements = wait.until(visibilityOfAllElementsLocatedBy(By.cssSelector(".footer__links .custom-page__link")));
                assertThat(footerElements).hasSize(links.length)
                                          .extracting(elm -> elm.getAttribute("data-hx-get"))
                                          .containsExactlyInAnyOrder(links);
                break;

            default:
                break;
        }
    }

    void _assertLoadMoreVisibility(boolean visible) {
        var btnLoadMore = driver.findElements(cssSelector("#more-posts"));
        if (visible) {
            assertThat(btnLoadMore).isNotEmpty().hasSize(1);
        } else {
            assertThat(btnLoadMore).isEmpty();
        }
    }

    private void _assertNumberOfPosts(int numberOfPosts) {
        await().alias("Wait for %d posts...".formatted(numberOfPosts))
               .until(() -> {
                   var gridElements = driver.findElements(By.cssSelector("article.article-card"));
                   var featuredPost = driver.findElements(By.cssSelector(".featured .featured__grid"));
                   return gridElements.size() + featuredPost.size() == numberOfPosts;
               });
    }

    private void _click(PagePlacement placement, String link) {
        switch (placement) {
            case FOOTER:
                var footerLink =
                        wait.until(visibilityOfElementLocated(By.cssSelector(".footer__links .custom-page__link[data-hx-get=\"%s\"]".formatted(link))));
                reliableClick(footerLink);
                break;

            default:
                break;
        }
    }

    private void _clickFirstPostTitle() {
        var firstCard = wait.until(visibilityOfElementLocated(cssSelector(".article-card")));
        var titleLink = firstCard.findElement(cssSelector(".article-card__title a"));

        String hxGet = titleLink.getAttribute("data-hx-get");
        assertThat(hxGet).isNotNull();

        // Click the link (it uses htmx, so the main content should update)
        reliableClick(titleLink);
        waitForReady();
    }

    private void _goTo(String url) {
        driver.navigate().to(rootUri + url);
        waitForReady();
        syncCsrfCookieFromPage();
    }

    private void _loadMore() {
        var loadMoreBtn = wait.until(elementToBeClickable(cssSelector("#more-posts button")));
        reliableClick(loadMoreBtn);
        waitForReady();
    }

    private void _logout() {
        driver.manage().deleteCookieNamed(dev.vepo.contraponto.components.forms.LoginEndpoint.SESSION_COOKIE_NAME);
        driver.navigate().refresh();
        waitForReady();
    }

    public App access() {
        driver.get(this.rootUri);
        return this;
    }

    public ProfilePage accountSecurity() {
        _goTo("/account/security");
        return new ProfilePage();
    }

    public App assertAccessButtonIsDisplayed() {
        waitForReady();
        var loginBtn = wait.until(visibilityOfElementLocated(cssSelector("button.btn--auth-login")));
        assertThat(loginBtn.isDisplayed()).isTrue();
        return this;
    }

    public App assertBreadcrumb(String... labels) {
        var items = wait.until(visibilityOfAllElementsLocatedBy(cssSelector(".breadcrumb__item")));
        assertThat(items).hasSize(labels.length);
        for (int i = 0; i < labels.length; i++) {
            String text;
            if (i == labels.length - 1) {
                text = items.get(i).findElement(cssSelector(".breadcrumb__current")).getText().trim();
            } else if (!items.get(i).findElements(cssSelector(".breadcrumb__link")).isEmpty()) {
                text = items.get(i).findElement(cssSelector(".breadcrumb__link")).getText().trim();
            } else {
                text = items.get(i).findElement(cssSelector(".breadcrumb__text")).getText().trim();
            }
            assertThat(text).isEqualTo(labels[i]);
        }
        return this;
    }

    public App assertCookieIsPresent(String key) {
        assertThat(driver.manage()
                         .getCookieNamed(key)).isNotNull()
                                              .extracting(Cookie::getName)
                                              .asString()
                                              .isNotBlank();
        return this;
    }

    public App assertFeaturedDisplayed() {
        var articles = driver.findElements(cssSelector(".article-card, .featured"));
        assertThat(articles).isNotEmpty();
        return this;
    }

    public App assertHeaderIsDisplayed() {
        var header = wait.until(visibilityOfElementLocated(className("site-header")));
        assertThat(header.isDisplayed()).isTrue();
        return this;
    }

    public App assertHubNavDoesNotContain(String label) {
        for (var link : driver.findElements(cssSelector(".hub-nav__link"))) {
            assertThat(link.getText().trim()).isNotEqualTo(label);
        }
        return this;
    }

    public App assertLinks(PagePlacement placement, String... links) {
        _assertLinks(placement, links);
        return this;
    }

    public App assertLoadMoreIsNotVisible() {
        _assertLoadMoreVisibility(false);
        return this;
    }

    public App assertLoadMoreIsVisible() {
        _assertLoadMoreVisibility(true);
        return this;
    }

    public App assertMainContent() {
        var main = driver.findElement(By.tagName("main"));
        assertThat(main.isDisplayed()).isTrue();
        return this;
    }

    public App assertMenuIsDisplayed() {
        var userMenu = wait.until(visibilityOfElementLocated(className("user-menu")));
        assertThat(userMenu.isDisplayed()).isTrue();
        return this;
    }

    public App assertNoFeaturedCard() {
        waitForReady();
        var featured = driver.findElements(cssSelector(".featured"));
        assertThat(featured).isEmpty();
        return this;
    }

    public App assertNotPostMessage() {
        var emptyMessage = wait.until(visibilityOfElementLocated(cssSelector(".user-blog__empty")));
        assertThat(emptyMessage.getText()).contains("No posts published yet");
        return this;
    }

    public App assertNumberOfPosts(int numberOfPosts) {
        _assertNumberOfPosts(numberOfPosts);
        return this;
    }

    public App assertPageTopPresent() {
        wait.until(visibilityOfElementLocated(cssSelector(".page-top")));
        wait.until(visibilityOfElementLocated(cssSelector(".container-narrow .breadcrumb")));
        wait.until(visibilityOfElementLocated(cssSelector(".page-top__actions")));
        return this;
    }

    public App assertPostTitles(List<String> titles) {
        var titlesElements = driver.findElements(By.cssSelector(".article-card__title, .featured__title"));
        assertThat(titlesElements).hasSize(titles.size())
                                  .extracting(WebElement::getText)
                                  .extracting(String::trim)
                                  .containsExactly(titles.stream().toArray(String[]::new));
        return this;
    }

    public App assertSingleMainElement() {
        assertThat(driver.findElements(By.tagName("main"))).hasSize(1);
        return this;
    }

    public App assertUrl(String url) {
        wait.until(urlToBe(this.rootUri + url));
        return this;
    }

    public BlogManagePage blogs() {
        return writingBlogs();
    }

    public App clearAuth() {
        driver.manage().deleteAllCookies();
        access();
        return this;
    }

    public App click(PagePlacement placement, String link) {
        _click(placement, link);
        return this;
    }

    public BlogPage clickFirstPostTitle() {
        _clickFirstPostTitle();
        return new BlogPage();
    }

    @Deprecated
    public App clickHubCard(String path) {
        reliableClick(wait.until(elementToBeClickable(
                                                      cssSelector("a.nav-hub__card[data-hx-get='%s']".formatted(path)))));
        waitForReady();
        return this;
    }

    public App clickHubSection(String hubBasePath, String sectionSlug) {
        reliableClick(wait.until(elementToBeClickable(
                                                      cssSelector(".hub-nav a.hub-nav__link[data-hx-get='%s/%s']"
                                                                                                                 .formatted(hubBasePath, sectionSlug)))));
        waitForReady();
        return this;
    }

    public App clickMenuLink(String path) {
        openUserMenu();
        var selector = "#userDropdown a[data-hx-get='%s']".formatted(path);
        var link = wait.until(presenceOfElementLocated(cssSelector(selector)));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", link);
        waitForReady();
        return this;
    }

    public App clickNotificationBell() {
        reliableClick(wait.until(elementToBeClickable(By.id("notification-bell"))));
        waitForReady();
        return this;
    }

    public CommentManagePage comments() {
        _goTo("/manage/comments");
        return new CommentManagePage();
    }

    public CustomPageManagePage customPages() {
        _goTo("/manage/pages");
        return new CustomPageManagePage();
    }

    public DashboardPage dashboard() {
        _goTo("/manage/dashboard");
        return new DashboardPage();
    }

    public WritePage editDraft(Long draftId) {
        _goTo("/write/draft/" + draftId);
        return new WritePage();
    }

    public Featured featuredCard() {
        return new Featured(wait.until(visibilityOfElementLocated(cssSelector(".featured"))));
    }

    public BlogPage goTo(Blog blog) {
        _goTo(TemplateExtensions.url(blog));
        return new BlogPage();
    }

    public PostPage goTo(Post post) {
        _goTo(TemplateExtensions.url(post));
        waitForReady();
        return new PostPage();
    }

    public DashboardPage goToAnalyticsFragment(long blogId, boolean compare) {
        var path = "/manage/dashboard/components/analytics?blogId=" + blogId;
        if (compare) {
            path += "&compare=true";
        }
        _goTo(path);
        return new DashboardPage();
    }

    public ImageControlPage goToBlogImages(long blogId) {
        _goTo("/blogs/" + blogId + "/images");
        return new ImageControlPage();
    }

    public GitSyncHistoryPage goToGitSyncHistory(long blogId) {
        _goTo("/blogs/" + blogId + "/git-sync");
        return new GitSyncHistoryPage();
    }

    public GitSyncHistoryPage goToGitSyncRun(long blogId, long runId) {
        _goTo("/blogs/" + blogId + "/git-sync/" + runId);
        return new GitSyncHistoryPage();
    }

    public App goToPath(String path) {
        _goTo(path);
        waitForReady();
        return this;
    }

    public PostPage goToPost(User user, String slug) {
        driver.navigate().to(this.rootUri + "/" + user.getUsername() + "/post/" + slug);
        waitForReady();
        return new PostPage();
    }

    public ReviewPage goToReview() {
        _goTo("/editor/review");
        wait.until(visibilityOfElementLocated(cssSelector(".review-list")));
        return new ReviewPage();
    }

    public SerieBrowsePage goToSerie(String username, String serieSlug) {
        _goTo("/" + username + "/serie/" + serieSlug);
        waitForReady();
        return new SerieBrowsePage();
    }

    public TagBrowsePage goToTag(String slug) {
        _goTo("/tags/" + slug);
        waitForReady();
        return new TagBrowsePage();
    }

    public App loadMore() {
        _loadMore();
        return this;
    }

    public App login(User user) {
        var loggedUser = Given.inject(LoggedUserProvider.class)
                              .login(user);
        driver.manage().deleteAllCookies();
        access();
        driver.manage()
              .addCookie(new Cookie.Builder(LoginEndpoint.SESSION_COOKIE_NAME, loggedUser.getSessionId())
                                                                                                         .path("/")
                                                                                                         .build());
        _goTo("/");
        return this;
    }

    public Login loginModal() {
        var loginBtn = wait.until(elementToBeClickable(cssSelector("button.btn--auth-login")));
        reliableClick(loginBtn);
        return new Login();
    }

    public App logout() {
        _logout();
        return this;
    }

    public BlogManagePage manageBlogs() {
        _goTo("/manage/blogs");
        return new BlogManagePage();
    }

    public BlogManagePage newBlog() {
        _goTo("/blogs/new");
        return new BlogManagePage();
    }

    public UserManagePage newUser() {
        _goTo("/users/new");
        return new UserManagePage();
    }

    public App openNotificationsFromMenu() {
        return clickMenuLink("/account");
    }

    public App openUserMenu() {
        wait.until(elementToBeClickable(By.id("userMenuBtn")));
        ((JavascriptExecutor) driver).executeScript("""
                                                    const dropdown = document.getElementById('userDropdown');
                                                    const button = document.getElementById('userMenuBtn');
                                                    if (!dropdown || !button) {
                                                        return false;
                                                    }
                                                    if (!dropdown.classList.contains('user-menu__dropdown--open')) {
                                                        button.click();
                                                    }
                                                    return dropdown.classList.contains('user-menu__dropdown--open');
                                                    """);
        wait.until(d -> d.findElement(By.id("userDropdown"))
                         .getAttribute("class")
                         .contains("user-menu__dropdown--open"));
        return this;
    }

    public PasswordRecoveryPage passwordRecovery() {
        _goTo("/password-recovery");
        return new PasswordRecoveryPage();
    }

    public ProfilePage profile() {
        _goTo("/account/security");
        waitForReady();
        return new ProfilePage();
    }

    /**
     * Scrolls the element into view and clicks; falls back to a JS click when
     * another layer intercepts the native click (fixed chrome, overlapping grid
     * quirks, etc.).
     */
    private void reliableClick(WebElement element) {
        ((JavascriptExecutor) driver).executeScript(
                                                    "arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});", element);
        wait.until(elementToBeClickable(element));
        try {
            element.click();
        } catch (ElementClickInterceptedException _) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
        }
    }

    public SearchModal searchModal() {
        reliableClick(wait.until(visibilityOfElementLocated(cssSelector("#searchBtn"))));
        return new SearchModal();
    }

    public SearchPage searchPage() {
        _goTo("/search");
        return new SearchPage();
    }

    private void syncCsrfCookieFromPage() {
        var token = (String) ((JavascriptExecutor) driver).executeScript("""
                                                                         var meta = document.querySelector('meta[name="csrf-token"]');
                                                                         return meta ? meta.content : '';
                                                                         """);
        if (token == null || token.isBlank()) {
            return;
        }
        var existing = driver.manage().getCookieNamed(CsrfTokenService.COOKIE_NAME);
        if (existing != null && token.equals(existing.getValue())) {
            return;
        }
        driver.manage().deleteCookieNamed(CsrfTokenService.COOKIE_NAME);
        driver.manage()
              .addCookie(new Cookie.Builder(CsrfTokenService.COOKIE_NAME, token)
                                                                                .path("/")
                                                                                .build());
    }

    public TagManagePage tagsManage() {
        _goTo("/editor/tags");
        return new TagManagePage();
    }

    private void useFieldValue(String selector, String value) {
        var by = By.cssSelector(selector);
        await().atMost(Duration.ofSeconds(10))
               .pollInterval(Duration.ofMillis(50))
               .ignoreException(StaleElementReferenceException.class)
               .until(() -> {
                   var input = wait.until(visibilityOfElementLocated(by));
                   input.clear();
                   input.sendKeys(value);
                   return true;
               });
    }

    public UserManagePage users() {
        _goTo("/administration/users");
        return new UserManagePage();
    }

    public App visitBlog(String username) {
        _goTo("/" + username);
        waitForReady();
        return this;
    }

    public App visitPost(String username, String slug) {
        _goTo("/" + username + "/post/" + slug);
        waitForReady();
        return this;
    }

    public App waitForReady() {
        wait.until(d -> "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState")));
        wait.until(d -> Boolean.TRUE.equals(((JavascriptExecutor) d).executeScript(
                                                                                   "return typeof htmx === 'undefined' || !document.querySelector('.htmx-request');")));
        return this;
    }
    // Inside App class, after SearchPage

    // Inside App class, after LibraryPage

    public WritePage writePage() {
        _goTo("/write");
        return new WritePage();
    }

    public ProfilePage writingAppearance() {
        _goTo("/writing/appearance");
        return new ProfilePage();
    }

    public BlogManagePage writingBlogs() {
        _goTo("/writing/blogs");
        return new BlogManagePage();
    }
}
