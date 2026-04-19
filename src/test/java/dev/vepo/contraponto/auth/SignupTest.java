package dev.vepo.contraponto.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.openqa.selenium.By.className;
import static org.openqa.selenium.By.cssSelector;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;

import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;

@WebTest
@QuarkusTest
class SignupTest {

    @TestHTTPResource("/")
    URL testUrl;

    @BeforeEach
    void setup() {
        // Create an existing user to test duplicate email scenario
        Given.cleanup();
        Given.user()
             .withUsername("existing")
             .withEmail("existing@example.com")
             .withPassword("password123")
             .withName("Existing User")
             .persist();
    }

    @Test
    void signupModalValidationAndSuccess(WebDriver driver, WebDriverWait wait) {
        driver.get(testUrl.toString());
        // Open login modal and switch to signup
        var loginBtn = wait.until(visibilityOfElementLocated(className("auth-btn-login")));
        loginBtn.click();
        var signupLink = wait.until(visibilityOfElementLocated(cssSelector(".auth-form__switch a")));
        signupLink.click();

        var usernameInput = wait.until(visibilityOfElementLocated(cssSelector("input[name=\"username\"]")));
        var nameInput = driver.findElement(cssSelector("input[name=\"name\"]"));
        var emailInput = driver.findElement(cssSelector("input[name=\"email\"]"));
        var passwordInput = driver.findElement(cssSelector("input[name=\"password\"]"));
        var submitBtn = driver.findElement(cssSelector("button[type=\"submit\"]"));

        wait.until(d -> "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState")));
        assertThat(submitBtn.isEnabled()).isFalse();

        // Username validation (too short)
        usernameInput.sendKeys("ab");
        passwordInput.sendKeys("anyPassword123");
        var usernameError = driver.findElement(cssSelector(".form-group:has(input[name='username']) .error-message.min-value"));
        assertThat(usernameError.isDisplayed()).isTrue();
        assertThat(usernameError.getText()).contains("Username must be at least 3 characters.");
        assertThat(submitBtn.isEnabled()).isFalse();

        // Fix username
        usernameInput.clear();
        usernameInput.sendKeys("newuser");
        // Now fill other fields
        nameInput.sendKeys("New User");
        emailInput.sendKeys("new@example.com");
        passwordInput.sendKeys("password123");
        await().until(() -> submitBtn.isEnabled());

        // Submit
        submitBtn.click();

        // Modal closes, user menu appears
        var modalContainer = driver.findElement(By.className("modal__container"));
        await().until(() -> !modalContainer.isDisplayed());
        var userMenu = wait.until(visibilityOfElementLocated(className("user-menu")));
        assertThat(userMenu.isDisplayed()).isTrue();
    }

    @Test
    void duplicateEmailShowsErrorMessage(WebDriver driver, WebDriverWait wait) {
        driver.get(testUrl.toString());
        var loginBtn = wait.until(visibilityOfElementLocated(className("auth-btn-login")));
        loginBtn.click();

        var signupLink = wait.until(visibilityOfElementLocated(cssSelector(".auth-form__switch a")));
        signupLink.click();

        var usernameInput = wait.until(visibilityOfElementLocated(cssSelector("input[name=\"username\"]")));
        var nameInput = wait.until(visibilityOfElementLocated(cssSelector("input[name=\"name\"]")));
        var emailInput = driver.findElement(cssSelector("input[name=\"email\"]"));
        var passwordInput = driver.findElement(cssSelector("input[name=\"password\"]"));
        var submitBtn = driver.findElement(cssSelector("button[type=\"submit\"]"));

        // Fill with existing email
        usernameInput.sendKeys("duplicated");
        nameInput.sendKeys("Duplicate Tester");
        emailInput.sendKeys("existing@example.com");
        passwordInput.sendKeys("anyPassword123");
        await().until(() -> submitBtn.isEnabled());

        submitBtn.click();

        // Expect error message inside #authError or .response.error
        var authError = wait.until(visibilityOfElementLocated(cssSelector("#authModal .response.error")));
        assertThat(authError.getText()).contains("Email already registered");

        // Modal should still be open, submit button remains enabled
        var modalContainer = driver.findElement(By.className("modal__container"));
        assertThat(modalContainer.isDisplayed()).isTrue();
        assertThat(submitBtn.isEnabled()).isTrue();
    }

    @Test
    void invalidEmailFormatShowsErrorWithoutSubmitting(WebDriver driver, WebDriverWait wait) {
        driver.get(testUrl.toString());
        var loginBtn = wait.until(visibilityOfElementLocated(className("auth-btn-login")));
        loginBtn.click();

        var signupLink = wait.until(visibilityOfElementLocated(cssSelector(".auth-form__switch a")));
        signupLink.click();

        var emailInput = wait.until(visibilityOfElementLocated(cssSelector("input[name=\"email\"]")));
        var passwordInput = driver.findElement(cssSelector("input[name=\"password\"]"));
        var submitBtn = driver.findElement(cssSelector("button[type=\"submit\"]"));

        wait.until(d -> "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState")));

        emailInput.sendKeys("not-an-email");
        passwordInput.sendKeys("anyPassword123");
        wait.until(d -> "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState")));

        var emailErrors = driver.findElements(cssSelector(".form-group:has(input[name='email']) .error-message"))
                                .stream()
                                .filter(WebElement::isDisplayed)
                                .findFirst();
        assertThat(emailErrors.isPresent());
        var emailError = emailErrors.get();
        assertThat(emailError.isDisplayed()).isTrue();
        assertThat(emailError.getText()).contains("valid email");
        assertThat(submitBtn.isEnabled()).isFalse();

        // Fix email
        emailInput.clear();
        emailInput.sendKeys("good@example.com");
        // Error should disappear
        await().until(() -> !emailError.isDisplayed());
        // Now button may still be disabled because password empty
        assertThat(submitBtn.isEnabled()).isFalse();
    }

    @Test
    void missingNameShowsErrorMessage(WebDriver driver, WebDriverWait wait) {
        driver.get(testUrl.toString());
        var loginBtn = wait.until(visibilityOfElementLocated(className("auth-btn-login")));
        loginBtn.click();

        var signupLink = wait.until(visibilityOfElementLocated(cssSelector(".auth-form__switch a")));
        signupLink.click();

        wait.until(d -> "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState")));

        var nameInput = wait.until(visibilityOfElementLocated(cssSelector("input[name=\"name\"]")));
        var emailInput = driver.findElement(cssSelector("input[name=\"email\"]"));
        var passwordInput = driver.findElement(cssSelector("input[name=\"password\"]"));
        var submitBtn = driver.findElement(cssSelector("button[type=\"submit\"]"));

        // 1. Fill email and password (they will become non‑pristine later)
        emailInput.sendKeys("test@example.com");
        passwordInput.sendKeys("password123");

        // 2. Make the name field non‑pristine:
        // - Focus, type something (changes value → hasChanged = true)
        // - Clear it (value becomes empty)
        // - Blur (sets pristine = false)
        nameInput.click();
        nameInput.sendKeys("temp");
        nameInput.clear();
        // Blur by clicking on email field (or any other)
        emailInput.sendKeys("any-email@example.com");

        // Now name field is empty and non‑pristine → error should appear
        WebElement nameError = driver.findElement(cssSelector(".form-group:has(input[name='name']) .error-message.required"));
        assertThat(nameError.isDisplayed()).isTrue();
        assertThat(nameError.getText()).contains("Name is required");
        assertThat(submitBtn.isEnabled()).isFalse();

        // 3. Fill name correctly → error disappears, button enabled
        nameInput.sendKeys("Valid Name");
        await().until(() -> !nameError.isDisplayed());
        await().until(() -> submitBtn.isEnabled());
    }
}