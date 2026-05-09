package dev.vepo.contraponto.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.openqa.selenium.By.className;
import static org.openqa.selenium.By.cssSelector;
import static org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfAllElementsLocatedBy;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import dev.vepo.contraponto.components.forms.LoginEndpoint;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.shared.infra.LoggedUserProvider;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResourceManager;

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
            assertThat(driver.findElement(By.className("modal__container")).isDisplayed()).isTrue();
            return (T) this;
        }

        public App assertModalWasClosed() {
            var modalContainer = driver.findElement(By.className("modal__container"));
            await().until(() -> !modalContainer.isDisplayed());
            return App.this;
        }

        public T assertNoFieldErrorMessage() {
            assertThat(driver.findElements(cssSelector(".form-group .error-message.visible"))).isEmpty();
            return (T) this;
        }

        public T assertSubmitDisabled() {
            var btn = wait.until(visibilityOfElementLocated(cssSelector("button[type=\"submit\"]")));
            await().until(() -> !btn.isEnabled());
            return (T) this;
        }

        public T assertSubmitEnabled() {
            var btn = wait.until(visibilityOfElementLocated(cssSelector("button[type=\"submit\"]")));
            await().until(btn::isEnabled);
            return (T) this;
        }

        public T submit() {
            driver.findElement(cssSelector("button[type=\"submit\"]"))
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

    public final class Login extends AccessModal<Login> {
        private Login() {}

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

        public App home() {
            var homeBtn = wait.until(visibilityOfElementLocated(By.cssSelector(".logo a")));
            homeBtn.click();
            waitForReady();
            return App.this;
        }

    }

    public class PostPage extends Page<PostPage> {
        private PostPage() {}

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

        public PostPage toggleFeatured() {
            var postFeaturedButton = driver.findElement(By.cssSelector("#post-featured-toggle"));
            postFeaturedButton.click();
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

    private final String rootUri;

    private final WebDriver driver;

    private final WebDriverWait wait;

    public App(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
        this.rootUri = TestHTTPResourceManager.getUri();
    }

    public App access() {
        driver.get(this.rootUri);
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

    public App assertMenuIsDisplayed() {
        var userMenu = wait.until(visibilityOfElementLocated(className("user-menu")));
        assertThat(userMenu.isDisplayed()).isTrue();
        return this;
    }

    public App assertNumberOfPosts(int numberOfPosts) {
        var gridElements = driver.findElements(By.cssSelector("article.article-card"));
        var featuredPost = driver.findElements(By.cssSelector(".featured .featured__grid"));
        assertThat(gridElements.size() + featuredPost.size()).isEqualTo(numberOfPosts);
        return this;
    }

    public PostPage goTo(Post post) {
        driver.navigate().to(rootUri + "/" + post.getAuthor().getUsername() + "/post/" + post.getSlug());
        waitForReady();
        return new PostPage();
    }

    public ReviewPage goToReview() {
        var userMenuBtn = wait.until(elementToBeClickable(className("user-menu__button")));
        userMenuBtn.click();
        var reviewBtn = wait.until(visibilityOfElementLocated(cssSelector(".user-menu__item[data-hx-get='/review']")));
        reviewBtn.click();
        waitForReady();
        return new ReviewPage();
    }

    public App login(User user) {
        var loggedUser = Given.inject(LoggedUserProvider.class)
                              .login(user);
        driver.manage()
              .addCookie(new Cookie(LoginEndpoint.SESSION_COOKIE_NAME, loggedUser.getSessionId()));
        driver.navigate().refresh();
        return this;
    }

    public Login loginModal() {
        wait.until(visibilityOfElementLocated(className("auth-btn-login")))
            .click();
        return new Login();
    }

    public App logout() {
        driver.manage()
              .deleteAllCookies();
        driver.navigate().refresh();
        return this;
    }

    private void useFieldValue(String cssSelector, String value) {
        var input = wait.until(visibilityOfElementLocated(cssSelector(cssSelector)));
        input.clear();
        input.sendKeys(value);
    }

    public App waitForReady() {
        wait.until(d -> "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState")));
        return this;
    }
}
