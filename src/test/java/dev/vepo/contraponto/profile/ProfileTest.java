package dev.vepo.contraponto.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.openqa.selenium.By.className;
import static org.openqa.selenium.By.cssSelector;
import static org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable;
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
class ProfileTest {

    private static final String TEST_USER_EMAIL = "profile@example.com";

    private static final String TEST_USER_PASSWORD = "profilePass123";
    private static final String TEST_USER_USERNAME = "profileuser";
    private static final String TEST_USER_NAME = "Profile Tester";
    @TestHTTPResource("/")
    URL testUrl;

    @Test
    void authenticatedUserCanViewProfile(WebDriver driver, WebDriverWait wait) {
        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);

        // Navigate to profile page
        var userMenuBtn = wait.until(visibilityOfElementLocated(className("user-menu__button")));
        userMenuBtn.click();
        var profileLink = wait.until(visibilityOfElementLocated(cssSelector(".user-menu__item[data-hx-get='/profile']")));
        profileLink.click();

        // Profile page should load
        var profileForm = wait.until(visibilityOfElementLocated(className("profile-form")));
        assertThat(profileForm.isDisplayed()).isTrue();

        // Check that fields are pre-filled with user data
        var nameInput = profileForm.findElement(cssSelector("input[name='name']"));
        var emailInput = profileForm.findElement(cssSelector("input[name='email']"));
        assertThat(nameInput.getAttribute("value")).isEqualTo(TEST_USER_NAME);
        assertThat(emailInput.getAttribute("value")).isEqualTo(TEST_USER_EMAIL);
    }

    @Test
    void changePasswordSuccessfully(WebDriver driver, WebDriverWait wait) {
        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "profile");

        var currentPasswordInput = wait.until(visibilityOfElementLocated(cssSelector("input[name='currentPassword']")));
        var newPasswordInput = driver.findElement(cssSelector("input[name='newPassword']"));
        var confirmPasswordInput = driver.findElement(cssSelector("input[name='confirmPassword']"));
        var submitBtn = driver.findElement(cssSelector("button[type='submit']"));

        currentPasswordInput.sendKeys(TEST_USER_PASSWORD);
        newPasswordInput.sendKeys("newPassword456");
        confirmPasswordInput.sendKeys("newPassword456");

        submitBtn.click();

        var successMsg = wait.until(visibilityOfElementLocated(cssSelector("#profileMessage .success-message")));
        assertThat(successMsg.getText()).contains("Profile updated successfully");

        // Logout and login with new password
        var userMenuBtn = wait.until(elementToBeClickable(className("user-menu__button")));
        userMenuBtn.click();
        var logoutBtn = wait.until(visibilityOfElementLocated(cssSelector(".user-menu__item[hx-post='/forms/auth/logout']")));
        logoutBtn.click();

        // Login again with new password
        login(driver, wait, TEST_USER_EMAIL, "newPassword456");
        var userMenu = wait.until(visibilityOfElementLocated(className("user-menu")));
        assertThat(userMenu.isDisplayed()).isTrue();
    }

    private void login(WebDriver driver, WebDriverWait wait, String email, String password) {
        driver.get(testUrl.toString());
        var loginBtn = wait.until(visibilityOfElementLocated(className("auth-btn-login")));
        loginBtn.click();

        var loginInput = wait.until(visibilityOfElementLocated(cssSelector("input[name='login']")));
        var passwordInput = driver.findElement(cssSelector("input[name='password']"));
        var submitBtn = driver.findElement(cssSelector("button[type='submit']"));

        loginInput.sendKeys(email);
        passwordInput.sendKeys(password);
        await().until(() -> submitBtn.isEnabled());
        submitBtn.click();

        // Wait for modal to close and user menu to appear
        await().until(() -> !driver.findElement(By.id("authModal")).isDisplayed());
        wait.until(visibilityOfElementLocated(className("user-menu")));
    }

    @Test
    void logoutFromProfileRedirectsToHome(WebDriver driver, WebDriverWait wait) {
        // 1. Log in as the test user
        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);

        // 2. Navigate directly to the profile page
        driver.get(testUrl.toString() + "profile");

        // 3. Open the user menu and click logout
        var userMenuBtn = wait.until(elementToBeClickable(className("user-menu__button")));
        userMenuBtn.click();
        var logoutBtn = wait.until(visibilityOfElementLocated(cssSelector(".user-menu__item[hx-post='/forms/auth/logout']")));
        logoutBtn.click();

        // 4. After logout, the user should be on the home page.
        // Wait for the "Sign In" button to appear (indicates logged out state)
        wait.until(visibilityOfElementLocated(className("auth-btn-login")));

        // 5. Assert the URL is the base home URL
        assertThat(driver.getCurrentUrl()).isEqualTo(testUrl.toString());

        // 6. Assert that the profile form is no longer present in the DOM
        var profileForm = driver.findElements(cssSelector(".profile-form"));
        assertThat(profileForm).isEmpty();
    }

    @Test
    void passwordMismatchShowsError(WebDriver driver, WebDriverWait wait) {
        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "profile");

        var currentPasswordInput = wait.until(visibilityOfElementLocated(cssSelector("input[name='currentPassword']")));
        var newPasswordInput = driver.findElement(cssSelector("input[name='newPassword']"));
        var confirmPasswordInput = driver.findElement(cssSelector("input[name='confirmPassword']"));
        var submitBtn = driver.findElement(cssSelector("button[type='submit']"));

        currentPasswordInput.sendKeys(TEST_USER_PASSWORD);
        newPasswordInput.sendKeys("newPassword456");
        confirmPasswordInput.sendKeys("differentPassword");

        submitBtn.click();

        var errorMsg = wait.until(visibilityOfElementLocated(cssSelector("#profileMessage .error-message")));
        assertThat(errorMsg.getText()).contains("Passwords do not match");
    }

    @BeforeEach
    void setup() {
        Given.cleanup();
        Given.user()
             .withUsername(TEST_USER_USERNAME)
             .withEmail(TEST_USER_EMAIL)
             .withPassword(TEST_USER_PASSWORD)
             .withName(TEST_USER_NAME)
             .persist();
    }

    @Test
    void unauthenticatedUserCannotAccessProfile(WebDriver driver, WebDriverWait wait) {
        driver.get(testUrl.toString() + "profile");

        // Should be redirected to home or login; check that profile page is not shown
        // For a pure SPA with htmx, maybe it shows nothing? But typically backend
        // should block.
        // We'll check that the main content does NOT contain profile form.
        var profileForm = driver.findElements(cssSelector(".profile-form"));
        assertThat(profileForm).isEmpty();
        // Or check that an error message or login modal appears
        var loginModal = driver.findElements(By.id("authModal"));
        if (!loginModal.isEmpty()) {
            assertThat(loginModal.get(0).isDisplayed()).isTrue();
        }
    }

    @Test
    void updateProfileSuccessfully(WebDriver driver, WebDriverWait wait) {
        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);

        // Go to profile
        driver.get(testUrl.toString() + "profile");

        var nameInput = wait.until(visibilityOfElementLocated(cssSelector("input[name='name']")));
        var emailInput = driver.findElement(cssSelector("input[name='email']"));
        var currentPasswordInput = driver.findElement(cssSelector("input[name='currentPassword']"));
        var submitBtn = driver.findElement(cssSelector("button[type='submit']"));

        // Update name and email
        var newName = "Updated Name";
        var newEmail = "updated@example.com";
        nameInput.clear();
        nameInput.sendKeys(newName);
        emailInput.clear();
        emailInput.sendKeys(newEmail);
        currentPasswordInput.sendKeys(TEST_USER_PASSWORD);

        submitBtn.click();

        // Wait for success message
        var successMsg = wait.until(visibilityOfElementLocated(cssSelector("#profileMessage .success-message")));
        assertThat(successMsg.getText()).contains("Profile updated successfully");

        // Reload page to verify persistence
        driver.navigate().refresh();
        nameInput = wait.until(visibilityOfElementLocated(cssSelector("input[name='name']")));
        emailInput = driver.findElement(cssSelector("input[name='email']"));
        assertThat(nameInput.getAttribute("value")).isEqualTo(newName);
        assertThat(emailInput.getAttribute("value")).isEqualTo(newEmail);
    }

    @Test
    void updateProfileWithDuplicateEmailShowsError(WebDriver driver, WebDriverWait wait) {
        // Create another user with a different email
        Given.user()
             .withUsername("otheruser")
             .withEmail("other@example.com")
             .withPassword("otherPass")
             .withName("Other User")
             .persist();

        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "profile");

        var emailInput = wait.until(visibilityOfElementLocated(cssSelector("input[name='email']")));
        var currentPasswordInput = driver.findElement(cssSelector("input[name='currentPassword']"));
        var submitBtn = driver.findElement(cssSelector("button[type='submit']"));

        emailInput.clear();
        emailInput.sendKeys("other@example.com");
        currentPasswordInput.sendKeys(TEST_USER_PASSWORD);

        submitBtn.click();

        var errorMsg = wait.until(visibilityOfElementLocated(cssSelector("#profileMessage .error-message")));
        assertThat(errorMsg.getText()).contains("Email already registered");
    }

    @Test
    void updateProfileWithWrongCurrentPasswordShowsError(WebDriver driver, WebDriverWait wait) {
        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "profile");

        var nameInput = wait.until(visibilityOfElementLocated(cssSelector("input[name='name']")));
        var currentPasswordInput = driver.findElement(cssSelector("input[name='currentPassword']"));
        var submitBtn = driver.findElement(cssSelector("button[type='submit']"));

        nameInput.clear();
        nameInput.sendKeys("Any Name");
        currentPasswordInput.sendKeys("wrongPassword");

        submitBtn.click();

        var errorMsg = wait.until(visibilityOfElementLocated(cssSelector("#profileMessage .error-message")));
        assertThat(errorMsg.getText()).contains("Current password is incorrect");
    }
}