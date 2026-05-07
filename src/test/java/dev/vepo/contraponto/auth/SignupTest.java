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
import org.openqa.selenium.support.ui.WebDriverWait;

import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;

@WebTest
@QuarkusTest
class SignupTest {

    @TestHTTPResource("/")
    URL testUrl;

    @Test
    void duplicateEmailShowsErrorMessage(App app) {
        app.access()
           .loginModal()
           .switchToSignup()
           .useUsername("duplicated")
           .useName("Duplicate Tester")
           // Fill with existing email
           .useEmail("existing@example.com")
           .usePassword("anyPassword123")
           .assertSubmitEnabled()
           .submit()
           // Expect error message inside #authError or .response.error
           .assertErrorMessage("Email already registered")
           // Modal should still be open, submit button remains enabled
           .assertModalIsOpen()
           .assertSubmitEnabled();
    }

    @Test
    void invalidEmailFormatShowsErrorWithoutSubmitting(App app) {
        app.access()
           .loginModal()
           .switchToSignup()
           // load validation scripts
           .waitForReady()
           .useEmail("not-an-email")
           .usePassword("anyPassword123")
           // wait validation ends
           .waitForReady()
           .assertFieldError("Please enter a valid email address.")
           .assertSubmitDisabled()
           // Fix email
           .useEmail("good@example.com")
           // Error should disappear
           .assertNoFieldErrorMessage()
           // Now button may still be disabled because other fields are emtpy
           .assertSubmitDisabled();
    }

    @Test
    void missingNameShowsErrorMessage(App app) {
        app.access()
           .loginModal()
           .switchToSignup()
           .waitForReady()
           // 1. Fill email and password (they will become non‑pristine later)
           .usePassword("password123")
           // 2. Make the name field non‑pristine:
           // - Focus, type something (changes value → hasChanged = true)
           // - Clear it (value becomes empty)
           // - Blur (sets pristine = false)
           .useName("temp")
           // Blur by clicking on email field (or any other)
           .useEmail("any-email@example.com")
           .useName("")
           // Now name field is empty and non‑pristine → error should appear
           .waitForReady()
           .assertFieldError("Name is required.")
           .assertSubmitDisabled()
           // 3. Fill name correctly → error disappears, button enabled
           .useName("Valid Name")
           .useUsername("validauser")
           .waitForReady()
           .assertNoFieldErrorMessage()
           .assertSubmitEnabled();
    }

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
}