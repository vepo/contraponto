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
    public class Login {

        public Signup switchToSignup() {
            wait.until(visibilityOfElementLocated(cssSelector(".auth-form__switch a"))).click();
            return new Signup();
        }
    }

    public class Signup {

        public Signup assertErrorMessage(String errorMessage) {
            var authError = wait.until(visibilityOfElementLocated(cssSelector("#authModal .response.error")));
            assertThat(authError.getText()).contains(errorMessage);
            return this;
        }

        public Signup assertFieldError(String... errorMessages) {
            var errors = driver.findElements(cssSelector(".form-group .error-message.visible"));
            assertThat(errors).hasSize(errorMessages.length)
                              .allMatch(WebElement::isDisplayed)
                              .extracting(WebElement::getText)
                              .containsExactlyInAnyOrder(errorMessages);
            return this;
        }

        public Signup assertModalIsOpen() {
            assertThat(driver.findElement(By.className("modal__container")).isDisplayed()).isTrue();
            return this;
        }

        public App assertModalWasClosed() {
            var modalContainer = driver.findElement(By.className("modal__container"));
            await().until(() -> !modalContainer.isDisplayed());
            return App.this;
        }

        public Signup assertNoFieldErrorMessage() {
            assertThat(driver.findElements(cssSelector(".form-group .error-message.visible"))).isEmpty();
            return this;
        }

        public Signup assertSubmitDisabled() {
            var btn = driver.findElement(cssSelector("button[type=\"submit\"]"));
            await().until(() -> !btn.isEnabled());
            return this;
        }

        public Signup assertSubmitEnabled() {
            var btn = driver.findElement(cssSelector("button[type=\"submit\"]"));
            await().until(() -> btn.isEnabled());
            return this;
        }

        public Signup submit() {
            driver.findElement(cssSelector("button[type=\"submit\"]"))
                  .click();
            return this;
        }

        public Signup useEmail(String email) {
            useFieldValue("input[name=\"email\"]", email);
            return this;
        }

        private void useFieldValue(String cssSelector, String value) {
            var input = wait.until(visibilityOfElementLocated(cssSelector(cssSelector)));
            input.clear();
            input.sendKeys(value);
        }

        public Signup useName(String name) {
            useFieldValue("input[name=\"name\"]", name);
            return this;
        }

        public Signup usePassword(String password) {
            useFieldValue("input[name=\"password\"]", password);
            return this;
        }

        public Signup useUsername(String username) {
            useFieldValue("input[name=\"username\"]", username);
            return this;
        }

        public Signup waitForReady() {
            App.this.waitForReady();
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

    public App waitForReady() {
        wait.until(d -> "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState")));
        return this;
    }

}
