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
import org.openqa.selenium.WebDriver;
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

        // 1. Open the login modal
        var loginBtn = wait.until(visibilityOfElementLocated(className("auth-btn-login")));
        assertThat(loginBtn.isEnabled()).isTrue();
        loginBtn.click();

        // 2. Switch to signup mode
        var signupLink = wait.until(visibilityOfElementLocated(cssSelector(".auth-form__switch a")));
        assertThat(signupLink.getText()).contains("Sign Up");
        signupLink.click();

        // Wait for the modal content to reload with signup form
        var nameInput = wait.until(visibilityOfElementLocated(cssSelector("input[name=\"name\"]")));
        var emailInput = driver.findElement(cssSelector("input[name=\"email\"]"));
        var passwordInput = driver.findElement(cssSelector("input[name=\"password\"]"));
        var submitBtn = driver.findElement(cssSelector("button[type=\"submit\"]"));

        // 3. Initially submit button should be disabled (empty fields)
        assertThat(submitBtn.isEnabled()).isFalse();

        // 4. Fill only name -> still disabled, name error should not appear yet (or
        // after blur)
        nameInput.sendKeys("New User");
        emailInput.click(); // blur name
        // Name error message should appear if name is required? Usually not until
        // submit or blur.
        // But we can check that button remains disabled because email/password empty.
        assertThat(submitBtn.isEnabled()).isFalse();

        // 5. Fill invalid email (no '@')
        emailInput.sendKeys("invalid-email");
        passwordInput.click(); // blur email
        var emailError = driver.findElement(cssSelector(".form-group:has(input[name='email']) .error-message"));
        assertThat(emailError.isDisplayed()).isTrue();
        assertThat(emailError.getText()).contains("valid email");
        assertThat(submitBtn.isEnabled()).isFalse();

        // 6. Fill valid email but empty password
        emailInput.clear();
        emailInput.sendKeys("test@example.com");
        passwordInput.click(); // blur email, email error should disappear
        assertThat(emailError.isDisplayed()).isFalse();

        // Password error should appear
        var passwordError = driver.findElement(cssSelector(".form-group:has(input[name='password']) .error-message"));
        assertThat(passwordError.isDisplayed()).isTrue();
        assertThat(passwordError.getText()).contains("Password");
        assertThat(submitBtn.isEnabled()).isFalse();

        // 7. Fill valid password -> button enabled, all errors hidden
        passwordInput.sendKeys("validPassword123");
        await().until(() -> submitBtn.isEnabled());
        assertThat(passwordError.isDisplayed()).isFalse();

        // 8. Submit the form
        submitBtn.click();

        // After successful signup:
        // - Modal should close
        var modalContainer = driver.findElement(By.className("modal__container"));
        await().until(() -> !modalContainer.isDisplayed());

        // - Menu should be reloaded (user-menu appears)
        var userMenu = wait.until(visibilityOfElementLocated(className("user-menu")));
        assertThat(userMenu.isDisplayed()).isTrue();

        // - Session cookie is set (using old cookie name, adjust if encrypted)
        var sessionCookie = driver.manage().getCookieNamed("__session");
        assertThat(sessionCookie).isNotNull();
        assertThat(sessionCookie.getValue()).isNotBlank();
    }

    @Test
    void duplicateEmailShowsErrorMessage(WebDriver driver, WebDriverWait wait) {
        driver.get(testUrl.toString());
        var loginBtn = wait.until(visibilityOfElementLocated(className("auth-btn-login")));
        loginBtn.click();

        var signupLink = wait.until(visibilityOfElementLocated(cssSelector(".auth-form__switch a")));
        signupLink.click();

        var nameInput = wait.until(visibilityOfElementLocated(cssSelector("input[name=\"name\"]")));
        var emailInput = driver.findElement(cssSelector("input[name=\"email\"]"));
        var passwordInput = driver.findElement(cssSelector("input[name=\"password\"]"));
        var submitBtn = driver.findElement(cssSelector("button[type=\"submit\"]"));

        // Fill with existing email
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

        emailInput.sendKeys("not-an-email");
        passwordInput.click(); // blur

        var emailError = driver.findElement(cssSelector(".form-group:has(input[name='email']) .error-message"));
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

        var nameInput = wait.until(visibilityOfElementLocated(cssSelector("input[name=\"name\"]")));
        var emailInput = driver.findElement(cssSelector("input[name=\"email\"]"));
        var passwordInput = driver.findElement(cssSelector("input[name=\"password\"]"));
        var submitBtn = driver.findElement(cssSelector("button[type=\"submit\"]"));

        // Leave name empty, fill other fields
        emailInput.sendKeys("test@example.com");
        passwordInput.sendKeys("password123");
        // Blur name field (it was never touched, but we can click on email to trigger
        // name blur if needed)
        nameInput.click();
        emailInput.click();

        // Name error should appear
        var nameError = driver.findElement(cssSelector(".form-group:has(input[name='name']) .error-message"));
        assertThat(nameError.isDisplayed()).isTrue();
        assertThat(nameError.getText()).contains("Name is required");
        assertThat(submitBtn.isEnabled()).isFalse();

        // Fill name
        nameInput.sendKeys("Valid Name");
        // Error should disappear, button enabled
        await().until(() -> !nameError.isDisplayed());
        await().until(() -> submitBtn.isEnabled());
    }
}