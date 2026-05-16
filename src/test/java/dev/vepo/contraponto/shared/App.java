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
import java.util.List;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.locators.RelativeLocator;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.components.forms.LoginEndpoint;
import dev.vepo.contraponto.custompage.PagePlacement;
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
            wait.until(elementToBeClickable(cssSelector("#authModal button[type=\"submit\"]")))
                .click();
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
            var title = wait.until(visibilityOfElementLocated(cssSelector(".pages-manage__title")));
            assertThat(title.getText()).isEqualTo(expected);
            return this;
        }

        public BlogManagePage assertToastSuccess(String message) {
            var toast = wait.until(visibilityOfElementLocated(cssSelector("#toast .toast--success")));
            assertThat(toast.getText()).contains(message);
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
            row.findElement(cssSelector(".btn--danger")).click();
            wait.until(d -> {
                try {
                    driver.switchTo().alert().accept();
                    return true;
                } catch (Exception e) {
                    return false;
                }
            });
            waitForReady();
            return this;
        }

        public BlogManagePage clickEdit(String title) {
            var row = findRowByTitle(title);
            row.findElement(By.linkText("Edit")).click();
            wait.until(visibilityOfElementLocated(cssSelector("#blogName")));
            return this;
        }

        public BlogManagePage clickNewBlog() {
            wait.until(visibilityOfElementLocated(cssSelector("a[data-hx-get='/blogs/new']"))).click();
            wait.until(visibilityOfElementLocated(cssSelector("#blogName")));
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
            wait.until(visibilityOfElementLocated(cssSelector("button[type='submit']"))).click();
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

        public BlogPage assertLoadMoreIsNotVisible() {
            _assertLoadMoreVisibility(false);
            return this;
        }

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

        public BlogPage loadMore() {
            _loadMore();
            return this;
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
            var title = wait.until(visibilityOfElementLocated(cssSelector(".pages-manage__title")));
            assertThat(title.getText()).isEqualTo(expected);
            return this;
        }

        public CustomPageManagePage assertToastSuccess(String message) {
            var toast = wait.until(visibilityOfElementLocated(cssSelector("#toast .toast--success")));
            assertThat(toast.getText()).contains(message);
            return this;
        }

        public CustomPageManagePage clickDelete(String title) {
            var row = findRowByTitle(title);
            row.findElement(cssSelector(".btn--danger")).click();
            wait.until(d -> {
                try {
                    driver.switchTo().alert().accept();
                    return true;
                } catch (Exception e) {
                    return false;
                }
            });
            waitForReady();
            return this;
        }

        public CustomPageManagePage clickEdit(String title) {
            var row = findRowByTitle(title);
            row.findElement(By.linkText("Edit")).click();
            wait.until(visibilityOfElementLocated(cssSelector("input[name='title']")));
            return this;
        }

        public CustomPageManagePage clickNewPage() {
            wait.until(visibilityOfElementLocated(cssSelector("a[data-hx-get='/pages/new']"))).click();
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
            wait.until(visibilityOfElementLocated(cssSelector("button[type='submit']"))).click();
            waitForReady();
            return this;
        }
    }

    public class DashboardPage extends Page<DashboardPage> {
        private DashboardPage() {}

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
            var title = wait.until(visibilityOfElementLocated(cssSelector(".dashboard-page__title")));
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

        public WritePage clickRecentDraft(int index) {
            var items = wait.until(visibilityOfAllElementsLocatedBy(cssSelector(".recent-section:first-child .recent-list__item")));
            assertThat(items).hasSizeGreaterThan(index);
            items.get(index).findElement(cssSelector(".recent-list__title")).click();
            waitForReady();
            return new WritePage();
        }

        public PostPage clickRecentPublished(int index) {
            var items = wait.until(visibilityOfAllElementsLocatedBy(cssSelector(".recent-section:last-child .recent-list__item")));
            assertThat(items).hasSizeGreaterThan(index);
            items.get(index).findElement(cssSelector(".recent-list__title")).click();
            waitForReady();
            return new PostPage();
        }

        public LibraryPage clickViewAllDrafts() {
            var link = wait.until(visibilityOfElementLocated(cssSelector(".stat-card:first-child .stat-card__link")));
            link.click();
            waitForReady();
            // After click, we expect the library page with drafts tab active
            return new LibraryPage();
        }

        public LibraryPage clickViewAllPublished() {
            var link = wait.until(visibilityOfElementLocated(cssSelector(".stat-card:last-child .stat-card__link")));
            link.click();
            waitForReady();
            return new LibraryPage();
        }

        public WritePage clickWriteNewStory() {
            var btn = wait.until(visibilityOfElementLocated(cssSelector(".dashboard-action .btn")));
            btn.click();
            waitForReady();
            return new WritePage();
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
            authorLink.click();
            wait.until(d -> d.getCurrentUrl().contains(hxGet));
            return new BlogPage();
        }

        public BlogPage accessPost() {
            var featuredTitle = elm.findElement(className("featured__title"));
            featuredTitle.click();
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

    public class LibraryPage extends Page<LibraryPage> {
        private LibraryPage() {}

        public LibraryPage assertDraftNotPresent(String title) {
            var drafts = driver.findElements(By.cssSelector(".drafts-list .draft-card__title"));
            assertThat(drafts).extracting(WebElement::getText)
                              .doesNotContain(title);
            return this;
        }

        public LibraryPage assertDraftPresent(String title) {
            assertThat(driver.getPageSource()).contains(title);
            return this;
        }

        public LibraryPage deleteFirstDraft() {
            var deleteBtn = wait.until(visibilityOfElementLocated(cssSelector(".draft-card .btn--danger")));
            deleteBtn.click();
            driver.switchTo().alert().accept();
            wait.until(d -> driver.findElements(cssSelector(".draft-card")).isEmpty());
            return this;
        }

        public LibraryPage switchTab(String tab) { // "drafts" or "published"
            var tabButton = wait.until(elementToBeClickable(cssSelector(".library-tab[data-tab='" + tab + "']")));
            tabButton.click();
            wait.until(visibilityOfElementLocated(cssSelector(".library-tab.library-tab--active[data-tab='" + tab + "']")));
            waitForReady();
            return this;
        }
    }

    public final class Login extends AccessModal<Login> {
        private Login() {}

        public Login closeModal() {
            var closeBtn = driver.findElement(By.cssSelector("#authModal .modal__close"));
            closeBtn.click();
            return this;
        }

        public Signup switchToSignup() {
            wait.until(visibilityOfElementLocated(cssSelector(".auth-form__switch a"))).click();
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

        public App home() {
            var homeBtn = wait.until(visibilityOfElementLocated(By.cssSelector(".logo a")));
            homeBtn.click();
            waitForReady();
            return App.this;
        }

        public App logout() {
            _logout();
            return App.this;
        }

        public T waitForReady() {
            App.this.waitForReady();
            return (T) this;
        }

    }

    public class PostPage extends Page<PostPage> {
        private PostPage() {}

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

        public PostPage assertPostTitle(String string) {
            // TODO
            return this;
        }

        public PostPage toggleFeatured() {
            var postFeaturedButton = driver.findElement(By.cssSelector("#post-featured-toggle"));
            postFeaturedButton.click();
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
            var errorMsg = wait.until(visibilityOfElementLocated(cssSelector("#profileMessage .error-message")));
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
            var successMsg = wait.until(visibilityOfElementLocated(cssSelector("#profileMessage .success-message")));
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
            wait.until(visibilityOfElementLocated(cssSelector("button[type='submit']"))).click();
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
            toggleBtn.click();
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
            closeBtn.click();
            wait.until(invisibilityOfElementLocated(By.id("searchModal")));
            return this;
        }

        public SearchPage goToAdvanced() {
            var modal = driver.findElement(By.id("searchModal"));
            var link = modal.findElement(cssSelector(".search-modal__advanced"));
            link.click();
            waitForReady();
            return new SearchPage();
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
            firstResultLink.click();
            waitForReady();
            return new PostPage();
        }

        public SearchPage loadMore() {
            var nextBtn = wait.until(elementToBeClickable(By.xpath("//button[contains(text(), 'Next')]")));
            nextBtn.click();
            waitForReady();
            return this;
        }

        public SearchPage search(String query) {
            var input = wait.until(visibilityOfElementLocated(cssSelector("input[name='q']")));
            input.sendKeys(query);
            var submitBtn = driver.findElement(cssSelector(".search-form__button"));
            submitBtn.click();
            waitForReady();
            return this;
        }

        public SearchPage submit() {
            var submitBtn = driver.findElement(cssSelector(".search-form__button"));
            submitBtn.click();
            waitForReady();
            return this;
        }

        public SearchPage type(String query) {
            var input = wait.until(visibilityOfElementLocated(cssSelector("input[name='q']")));
            input.sendKeys(query);
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

    public class UserManagePage extends Page<UserManagePage> {
        private UserManagePage() {}

        public UserManagePage assertManagePageNotLoaded() {
            assertThat(driver.findElements(cssSelector(".pages-manage__title"))).isEmpty();
            return this;
        }

        public UserManagePage assertTitle(String expected) {
            var title = wait.until(visibilityOfElementLocated(cssSelector(".pages-manage__title")));
            assertThat(title.getText()).isEqualTo(expected);
            return this;
        }

        public UserManagePage assertToastSuccess(String message) {
            var toast = wait.until(visibilityOfElementLocated(cssSelector("#toast .toast--success")));
            assertThat(toast.getText()).contains(message);
            return this;
        }

        public UserManagePage assertUserListed(String name) {
            var rows = wait.until(visibilityOfAllElementsLocatedBy(cssSelector(".pages-manage__row")));
            assertThat(rows).anyMatch(row -> row.findElement(cssSelector(".pages-manage__row-title")).getText().equals(name));
            return this;
        }

        public UserManagePage clickEdit(String name) {
            var row = findRowByTitle(name);
            row.findElement(By.linkText("Edit")).click();
            wait.until(visibilityOfElementLocated(cssSelector("#userName")));
            return this;
        }

        public UserManagePage clickNewUser() {
            wait.until(visibilityOfElementLocated(cssSelector("a[data-hx-get='/users/new']"))).click();
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
                checkbox.click();
            }
            return this;
        }

        public UserManagePage submit() {
            wait.until(visibilityOfElementLocated(cssSelector("button[type='submit']"))).click();
            waitForReady();
            return this;
        }
    }

    public class WritePage extends Page<WritePage> {
        private WritePage() {}

        public WritePage acceptAlertWithText(String text) {
            wait.until(d -> driver.switchTo().alert() != null);
            driver.switchTo().alert().sendKeys(text);
            driver.switchTo().alert().accept();
            return this;
        }

        public WritePage appendContent(String text) {
            var textarea = wait.until(visibilityOfElementLocated(cssSelector("#content")));
            textarea.sendKeys(text);
            return this;
        }

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

        public WritePage assertEditorMode(String expectedMode) {
            var modeButton = driver.findElement(cssSelector("#editorModeButton"));
            var label = modeButton.findElement(cssSelector(".editor-mode-label")).getText();
            assertThat(label).isEqualTo(expectedMode.equals("MARKDOWN") ? "Markdown" : "AsciiDoc");
            return this;
        }

        public WritePage assertHintContains(String text) {
            var hint = wait.until(visibilityOfElementLocated(cssSelector("#editorHint")));
            assertThat(hint.getText()).contains(text);
            return this;
        }

        public WritePage assertPreviewContains(String text) {
            var preview = driver.findElement(cssSelector("#previewContainer"));
            assertThat(preview.getText()).contains(text);
            return this;
        }

        public WritePage assertPreviewNotVisible() {
            var preview = wait.until(visibilityOfElementLocated(cssSelector("#previewContainer")));
            assertThat(preview.isDisplayed()).isFalse();
            return this;
        }

        public WritePage assertPreviewVisible() {
            var preview = wait.until(visibilityOfElementLocated(cssSelector("#previewContainer")));
            assertThat(preview.isDisplayed()).isTrue();
            return this;
        }

        public WritePage assertTitle(String title) {
            var input = wait.until(visibilityOfElementLocated(cssSelector("#title")));
            assertThat(input.getAttribute("value")).isEqualTo(title);
            return this;
        }

        public WritePage assertToastError(String message) {
            var toast = wait.until(visibilityOfElementLocated(cssSelector("#toast .toast--error")));
            assertThat(toast.getText()).contains(message);
            return this;
        }

        public WritePage assertToastSuccess(String message) {
            var toast = wait.until(visibilityOfElementLocated(cssSelector("#toast .toast--success")));
            assertThat(toast.getText()).contains(message);
            return this;
        }

        public WritePage clearContent() {
            var textarea = wait.until(visibilityOfElementLocated(cssSelector("#content")));
            textarea.clear();
            return this;
        }

        public WritePage clickToolbarButton(String command) {
            var button = wait.until(visibilityOfElementLocated(cssSelector("button[data-command='" + command + "']")));
            button.click();
            return this;
        }

        public WritePage fillContent(String content) {
            var textarea = wait.until(visibilityOfElementLocated(cssSelector("#content")));
            textarea.clear();
            textarea.sendKeys(content);
            return this;
        }

        public WritePage fillDescription(String description) {
            var input = wait.until(visibilityOfElementLocated(cssSelector("#description")));
            input.clear();
            input.sendKeys(description);
            return this;
        }

        public WritePage fillSlug(String slug) {
            var input = wait.until(visibilityOfElementLocated(cssSelector("#slug")));
            input.clear();
            input.sendKeys(slug);
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

        public WritePage injectMarkdownImage(String url) {
            ((JavascriptExecutor) driver).executeScript(
                                                        "document.getElementById('content').value += '![Image](" + url + ")';");
            return this;
        }

        public WritePage publish() {
            var publishBtn = wait.until(visibilityOfElementLocated(By.id("publish")));
            publishBtn.click();
            waitForToast();
            return this;
        }

        public WritePage removeCover() {
            var removeBtn = wait.until(visibilityOfElementLocated(By.id("removeCoverBtn")));
            removeBtn.click();
            wait.until(d -> !driver.findElement(By.id("coverPreview")).isDisplayed());
            return this;
        }

        public WritePage saveDraft() {
            var saveBtn = wait.until(visibilityOfElementLocated(By.id("saveDraft")));
            saveBtn.click();
            waitForToast(); // wait for success/error toast
            return this;
        }

        public WritePage selectTextInContent(int start, int end) {
            var textarea = driver.findElement(By.id("content"));
            ((JavascriptExecutor) driver).executeScript(
                                                        "arguments[0].setSelectionRange(arguments[1], arguments[2]);", textarea, start, end);
            return this;
        }

        public WritePage switchModeTo(String mode) { // "MARKDOWN" or "ASCIIDOC"
            var modeButton = wait.until(visibilityOfElementLocated(cssSelector("#editorModeButton")));
            modeButton.click();
            var option = wait.until(visibilityOfElementLocated(cssSelector("[data-mode='" + mode + "']")));
            option.click();
            wait.until(d -> modeButton.findElement(cssSelector(".editor-mode-label")).getText().equals(mode.equals("MARKDOWN") ? "Markdown" : "AsciiDoc"));
            return this;
        }

        public WritePage togglePreview() {
            var previewBtn = wait.until(visibilityOfElementLocated(cssSelector("#previewToggleBtn")));
            previewBtn.click();
            return this;
        }

        public WritePage uploadCover(Path imagePath) {
            var coverInput = wait.until(presenceOfElementLocated(By.id("coverInput")));
            coverInput.sendKeys(imagePath.toAbsolutePath().toString());
            wait.until(d -> driver.findElement(By.id("coverPreview")).isDisplayed());
            return this;
        }

        public WritePage waitForToast() {
            wait.until(d -> driver.findElements(cssSelector("#toast .toast--success, #toast .toast--error")).size() > 0);
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
                wait.until(visibilityOfElementLocated(By.cssSelector(".footer__links .custom-page__link[data-hx-get=\"%s\"]".formatted(link))))
                    .click();
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
        titleLink.click();
    }

    private void _goTo(String url) {
        driver.navigate().to(rootUri + url);
        waitForReady();
    }

    private void _loadMore() {
        var btnLoadMore = wait.until(elementToBeClickable(cssSelector("#more-posts")));
        btnLoadMore.click();
        waitForReady();
    }

    private void _logout() {
        var userMenuBtn = wait.until(elementToBeClickable(By.cssSelector("#userMenuBtn")));
        userMenuBtn.click();
        wait.until(visibilityOfElementLocated(By.cssSelector("#userDropdown.user-menu__dropdown--open")));
        var logoutBtn = wait.until(elementToBeClickable(By.cssSelector("button.user-menu__item[hx-post='/forms/auth/logout']")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", logoutBtn);
        logoutBtn.click();
        waitForReady();
    }

    public App access() {
        driver.get(this.rootUri);
        return this;
    }

    public App assertAccessButtonIsDisplayed() {
        var loginBtn = wait.until(visibilityOfElementLocated(cssSelector("button.btn--auth-login")));
        assertThat(loginBtn.isDisplayed()).isTrue();
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

    public App assertPostTitles(List<String> titles) {
        var titlesElements = driver.findElements(By.cssSelector(".article-card__title, .featured__title"));
        assertThat(titlesElements).hasSize(titles.size())
                                  .extracting(WebElement::getText)
                                  .extracting(String::trim)
                                  .containsExactly(titles.stream().toArray(String[]::new));
        return this;
    }

    public App assertUrl(String url) {
        wait.until(urlToBe(this.rootUri + url));
        return this;
    }

    public BlogManagePage blogs() {
        _goTo("/blogs");
        return new BlogManagePage();
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

    public CustomPageManagePage customPages() {
        _goTo("/pages");
        return new CustomPageManagePage();
    }

    public DashboardPage dashboard() {
        _goTo("/dashboard");
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

    public PostPage goToPost(User user, String slug) {
        driver.navigate().to(this.rootUri + "/" + user.getUsername() + "/post/" + slug);
        return new PostPage();
    }

    public ReviewPage goToReview() {
        var userMenuBtn = wait.until(elementToBeClickable(By.cssSelector("#userMenuBtn")));
        userMenuBtn.click();
        wait.until(visibilityOfElementLocated(By.cssSelector("#userDropdown.user-menu__dropdown--open")));
        var reviewBtn = wait.until(elementToBeClickable(cssSelector(".user-menu__item[data-hx-get='/review']")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", reviewBtn);
        reviewBtn.click();
        wait.until(visibilityOfElementLocated(cssSelector(".review-page")));
        waitForReady();
        return new ReviewPage();
    }

    public LibraryPage libraryPage() {
        _goTo("/library");
        return new LibraryPage();
    }

    public App loadMore() {
        _loadMore();
        return this;
    }

    public App login(User user) {
        var loggedUser = Given.inject(LoggedUserProvider.class)
                              .login(user);
        if (!driver.getCurrentUrl().contains(rootUri)) {
            access();
        }
        driver.manage()
              .addCookie(new Cookie(LoginEndpoint.SESSION_COOKIE_NAME, loggedUser.getSessionId()));
        driver.navigate().refresh();
        return this;
    }

    public Login loginModal() {
        wait.until(visibilityOfElementLocated(cssSelector("button.btn--auth-login")))
            .click();
        return new Login();
    }

    public App logout() {
        _logout();
        return this;
    }

    public BlogManagePage newBlog() {
        _goTo("/blogs/new");
        return new BlogManagePage();
    }

    public CustomPageManagePage newCustomPage() {
        _goTo("/pages/new");
        return new CustomPageManagePage();
    }

    public UserManagePage newUser() {
        _goTo("/users/new");
        return new UserManagePage();
    }

    public ProfilePage profile() {
        _goTo("/profile");
        return new ProfilePage();
    }

    public SearchModal searchModal() {
        wait.until(visibilityOfElementLocated(cssSelector("#searchBtn"))).click();
        return new SearchModal();
    }

    public SearchPage searchPage() {
        _goTo("/search");
        return new SearchPage();
    }

    private void useFieldValue(String cssSelector, String value) {
        var input = wait.until(visibilityOfElementLocated(cssSelector(cssSelector)));
        input.clear();
        input.sendKeys(value);
    }

    public UserManagePage users() {
        _goTo("/users");
        return new UserManagePage();
    }

    // Inside App class, after LibraryPage

    public App waitForReady() {
        wait.until(d -> "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState")));
        return this;
    }
    // Inside App class, after SearchPage

    public WritePage writePage() {
        _goTo("/write");
        return new WritePage();
    }
}
