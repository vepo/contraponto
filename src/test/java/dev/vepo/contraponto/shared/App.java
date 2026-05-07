package dev.vepo.contraponto.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.openqa.selenium.By.className;
import static org.openqa.selenium.By.cssSelector;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

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
            var btn = driver.findElement(cssSelector("button[type=\"submit\"]"));
            await().until(() -> !btn.isEnabled());
            return (T) this;
        }

        public T assertSubmitEnabled() {
            var btn = driver.findElement(cssSelector("button[type=\"submit\"]"));
            await().until(() -> btn.isEnabled());
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

    public App assertMenuIsDisplayed() {
        var userMenu = wait.until(visibilityOfElementLocated(className("user-menu")));
        assertThat(userMenu.isDisplayed()).isTrue();
        return this;
    }

    public Login loginModal() {
        wait.until(visibilityOfElementLocated(className("auth-btn-login")))
            .click();
        return new Login();
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
