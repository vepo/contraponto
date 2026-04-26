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

    @Test
    void invalidCredentialsShowsErrorMessage(WebDriver driver, WebDriverWait wait) {
        driver.get(testUrl.toString());
        var loginBtn = wait.until(visibilityOfElementLocated(className("auth-btn-login")));
        loginBtn.click();

        var modalContainer = wait.until(visibilityOfElementLocated(className("modal__container")));
        var loginInput = wait.until(visibilityOfElementLocated(cssSelector("input[name=\"login\"]")));
        var passwordInput = driver.findElement(cssSelector("input[name=\"password\"]"));
        var submitBtn = driver.findElement(cssSelector("button[type=\"submit\"]"));

        // Fill with valid format but wrong credentials
        loginInput.sendKeys("wrong@example.com");
        passwordInput.sendKeys("wrongPassword");
        await().until(() -> submitBtn.isEnabled());

        submitBtn.click();

        // Expect an error message inside #authError (or a global error div)
        var authError = wait.until(visibilityOfElementLocated(cssSelector("#authModal .response.error")));
        assertThat(authError.getText()).contains("Invalid username/email or password.");
        // Modal should still be open
        assertThat(modalContainer.isDisplayed()).isTrue();
        // Submit button should remain enabled (user can retry)
        assertThat(submitBtn.isEnabled()).isTrue();
    }

    @Test
    void loginModalValidationAndSuccess(WebDriver driver, WebDriverWait wait) {
        driver.get(testUrl.toString());

        // Open login modal
        var loginBtn = wait.until(visibilityOfElementLocated(className("auth-btn-login")));
        loginBtn.click();

        var modalContainer = wait.until(visibilityOfElementLocated(className("modal__container")));
        assertThat(modalContainer.isDisplayed()).isTrue();
        var loginInput = wait.until(visibilityOfElementLocated(cssSelector("input[name=\"login\"]")));
        var passwordInput = driver.findElement(cssSelector("input[name=\"password\"]"));
        var submitBtn = driver.findElement(cssSelector("button[type=\"submit\"]"));

        // Initially disabled
        assertThat(submitBtn.isEnabled()).isFalse();

        // Test empty fields
        loginInput.sendKeys("");
        passwordInput.click();
        assertThat(submitBtn.isEnabled()).isFalse();

        // Test invalid login (non-existent)
        loginInput.clear();
        loginInput.sendKeys("nonexistent");
        passwordInput.sendKeys("validPassword123");
        await().until(() -> submitBtn.isEnabled());
        submitBtn.click();
        var authError = wait.until(visibilityOfElementLocated(cssSelector("#authModal .response.error")));
        assertThat(authError.getText()).contains("Invalid username/email");

        // Successful login with email
        loginInput.clear();
        loginInput.sendKeys("test@example.com");
        passwordInput.clear();
        passwordInput.sendKeys("validPassword123");
        await().until(() -> submitBtn.isEnabled());
        submitBtn.click();

        // Modal closes, user menu appears
        var postModalContainer = driver.findElement(By.className("modal__container"));
        await().until(() -> !postModalContainer.isDisplayed());
        var userMenu = wait.until(visibilityOfElementLocated(className("user-menu")));
        assertThat(userMenu.isDisplayed()).isTrue();

        // - Optionally check that the session cookie is set
        var sessionCookie = driver.manage().getCookieNamed("__session").getValue();
        assertThat(sessionCookie).isNotBlank();
    }

    @BeforeEach
    void setup() {
        Given.cleanup();
        Given.user()
             .withUsername("validuser")
             .withEmail("test@example.com")
             .withPassword("validPassword123")
             .withName("Valid User")
             .persist();
    }
}