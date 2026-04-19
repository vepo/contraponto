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
class LoginTest {

    @TestHTTPResource("/")
    URL testUrl;

    @BeforeEach
    void setup() {
        Given.user()
             .withUsername("valid")
             .withEmail("test@example.com")
             .withPassword("validPassword123")
             .withName("Valid User")
             .persist();
    }

    @Test
    void loginModalValidationAndSuccess(WebDriver driver, WebDriverWait wait) {
        driver.get(testUrl.toString());

        // 1. Open the login modal
        var loginBtn = wait.until(visibilityOfElementLocated(className("auth-btn-login")));
        assertThat(loginBtn.isEnabled()).isTrue();
        loginBtn.click();

        // Wait for modal to be present
        var modalContainer = wait.until(visibilityOfElementLocated(className("modal__container")));
        assertThat(modalContainer.isDisplayed()).isTrue();

        // Locate form fields and submit button
        var emailInput = wait.until(visibilityOfElementLocated(cssSelector("input[name=\"email\"]")));
        var passwordInput = driver.findElement(cssSelector("input[name=\"password\"]"));
        var submitBtn = driver.findElement(cssSelector("button[type=\"submit\"]"));

        // 2. Initially submit button should be disabled (empty fields)
        assertThat(submitBtn.isEnabled()).isFalse();

        // 3. Enter invalid email (no '@')
        emailInput.sendKeys("invalid-email");
        passwordInput.sendKeys(""); // ensure empty
        // Trigger validation (e.g., blur or input event)
        emailInput.click();
        passwordInput.click();

        // Check email error message appears
        var emailError = driver.findElement(cssSelector(".form-group:has(input[name='email']) .error-message"));
        assertThat(emailError.isDisplayed()).isTrue();
        assertThat(emailError.getText()).contains("valid email");
        // Submit button still disabled
        assertThat(submitBtn.isEnabled()).isFalse();

        // 4. Enter valid email but empty password
        emailInput.clear();
        emailInput.sendKeys("test@example.com");
        passwordInput.clear();
        passwordInput.click();

        // Password error message should appear
        var passwordError = driver.findElement(cssSelector(".form-group:has(input[name='password']) .error-message"));
        assertThat(passwordError.isDisplayed()).isTrue();
        assertThat(passwordError.getText()).contains("Password");
        assertThat(submitBtn.isEnabled()).isFalse();

        // 5. Enter valid password → button should become enabled, all errors hidden
        passwordInput.sendKeys("validPassword123");

        await().until(() -> submitBtn.isEnabled());
        // Both error messages should be hidden
        assertThat(emailError.isDisplayed()).isFalse();
        assertThat(passwordError.isDisplayed()).isFalse();

        // 6. Submit the form (successful login)
        submitBtn.click();

        // After successful login:
        // - Modal should close (disappear)
        var postModalContainer = driver.findElement(By.className(("modal__container")));
        await().until(() -> !postModalContainer.isDisplayed());
        // - Menu should be reloaded
        var userMenu = wait.until(visibilityOfElementLocated(className("user-menu")));
        assertThat(userMenu.isDisplayed()).isTrue();

        // - Optionally check that the session cookie is set
        var sessionCookie = driver.manage().getCookieNamed("__session").getValue();
        assertThat(sessionCookie).isNotBlank();
    }

    @Test
    void invalidCredentialsShowsErrorMessage(WebDriver driver, WebDriverWait wait) {
        driver.get(testUrl.toString());
        var loginBtn = wait.until(visibilityOfElementLocated(className("auth-btn-login")));
        loginBtn.click();

        var modalContainer = wait.until(visibilityOfElementLocated(className("modal__container")));
        var emailInput = wait.until(visibilityOfElementLocated(cssSelector("input[name=\"email\"]")));
        var passwordInput = driver.findElement(cssSelector("input[name=\"password\"]"));
        var submitBtn = driver.findElement(cssSelector("button[type=\"submit\"]"));

        // Fill with valid format but wrong credentials
        emailInput.sendKeys("wrong@example.com");
        passwordInput.sendKeys("wrongPassword");
        await().until(() -> submitBtn.isEnabled());

        submitBtn.click();

        // Expect an error message inside #authError (or a global error div)
        var authError = wait.until(visibilityOfElementLocated(cssSelector("#authModal .response.error")));
        assertThat(authError.getText()).contains("Invalid email or password");
        // Modal should still be open
        assertThat(modalContainer.isDisplayed()).isTrue();
        // Submit button should remain enabled (user can retry)
        assertThat(submitBtn.isEnabled()).isTrue();
    }
}