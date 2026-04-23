package dev.vepo.contraponto.write;

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
class WriteTest {

    @TestHTTPResource("/")
    URL testUrl;

    private static final String TEST_USER_EMAIL = "writer@example.com";
    private static final String TEST_USER_PASSWORD = "writerPass123";
    private static final String TEST_USER_USERNAME = "writeruser";
    private static final String TEST_USER_NAME = "Test Writer";

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

    // ------------------------------------------------------------------------
    // Access control tests
    // ------------------------------------------------------------------------

    @Test
    void unauthenticatedUserCannotAccessWritePage(WebDriver driver, WebDriverWait wait) {
        driver.get(testUrl.toString() + "write");

        // Should be redirected to home or show login modal; we expect the write page
        // not to load.
        // A simple check: the write page's main container should not be present.
        var writeForm = driver.findElements(cssSelector(".write-form"));
        assertThat(writeForm).isEmpty();

        // Optionally, the login modal may appear (or user remains on home with login
        // buttons)
        var loginModal = driver.findElements(By.id("authModal"));
        if (!loginModal.isEmpty()) {
            assertThat(loginModal.get(0).isDisplayed()).isTrue();
        }
    }

    @Test
    void authenticatedUserCanAccessWritePage(WebDriver driver, WebDriverWait wait) {
        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);

        driver.get(testUrl.toString() + "write");

        var writeForm = wait.until(visibilityOfElementLocated(cssSelector(".write-form")));
        assertThat(writeForm.isDisplayed()).isTrue();

        var titleInput = writeForm.findElement(cssSelector("#title"));
        assertThat(titleInput.isDisplayed()).isTrue();
    }

    // ------------------------------------------------------------------------
    // Creating a new post
    // ------------------------------------------------------------------------

    @Test
    void createNewPostAsDraft(WebDriver driver, WebDriverWait wait) {
        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "write");

        var title = "My Draft Post";
        var slug = "my-post";
        var description = "This is my post.";
        var content = "This is the content of my draft post.";

        var titleInput = wait.until(visibilityOfElementLocated(cssSelector("#title")));
        titleInput.sendKeys(title);
        var slugInput = wait.until(visibilityOfElementLocated(cssSelector("#slug")));
        slugInput.sendKeys(slug);
        var descriptionInput = wait.until(visibilityOfElementLocated(cssSelector("#description")));
        descriptionInput.sendKeys(description);
        var contentTextarea = driver.findElement(cssSelector("#content"));
        contentTextarea.sendKeys(content);

        // Click "Save Draft" button
        var saveDraftBtn = driver.findElement(By.id("saveDraft"));
        saveDraftBtn.click();

        // Wait for toast success message
        var toast = wait.until(visibilityOfElementLocated(cssSelector("#toast .toast--success")));
        assertThat(toast.getText()).contains("Draft saved successfully!");

        // After saving, URL should contain ?edit= with the new post id
        assertThat(driver.getCurrentUrl()).matches(".*/write\\?edit=\\d+");
    }

    @Test
    void publishNewPost(WebDriver driver, WebDriverWait wait) {
        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "write");

        String title = "Published Post";
        String content = "Published content here.";

        var titleInput = wait.until(visibilityOfElementLocated(cssSelector("#title")));
        titleInput.sendKeys(title);
        var contentTextarea = driver.findElement(cssSelector("#content"));
        contentTextarea.sendKeys(content);

        var publishBtn = driver.findElement(cssSelector("#publishBtn"));
        publishBtn.click();

        var toast = wait.until(visibilityOfElementLocated(cssSelector("#toast.toast--success")));
        assertThat(toast.getText()).contains("Post published!");

        // Verify we can navigate to the published post
        // The slug is generated from title; we assume it becomes "published-post"
        driver.get(testUrl.toString() + "post/published-post");
        var postTitle = wait.until(visibilityOfElementLocated(cssSelector(".article-page__title")));
        assertThat(postTitle.getText()).isEqualTo(title);
    }

    // ------------------------------------------------------------------------
    // Editing an existing post
    // ------------------------------------------------------------------------

    @Test
    void editExistingPost(WebDriver driver, WebDriverWait wait) {
        // First create a post directly via backend (or via UI and capture id)
        // Using Given to create a post for the user
        var postId = Given.post()
                          .withTitle("Original Title")
                          .withSlug("original-title")
                          .withContent("Original content")
                          .withAuthor(TEST_USER_EMAIL)
                          .persist()
                          .getId();

        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "write?edit=" + postId);

        var titleInput = wait.until(visibilityOfElementLocated(cssSelector("#title")));
        assertThat(titleInput.getAttribute("value")).isEqualTo("Original Title");

        // Edit title and content
        titleInput.clear();
        titleInput.sendKeys("Updated Title");
        var contentTextarea = driver.findElement(cssSelector("#content"));
        contentTextarea.clear();
        contentTextarea.sendKeys("Updated content");

        var publishBtn = driver.findElement(cssSelector("#publishBtn"));
        publishBtn.click();

        var toast = wait.until(visibilityOfElementLocated(cssSelector("#toast.toast--success")));
        assertThat(toast.getText()).contains("Post published!");

        // Verify on the post page
        driver.get(testUrl.toString() + "post/updated-title");
        var postTitle = wait.until(visibilityOfElementLocated(cssSelector(".article-page__title")));
        assertThat(postTitle.getText()).isEqualTo("Updated Title");
    }

    // ------------------------------------------------------------------------
    // Validation tests
    // ------------------------------------------------------------------------

    @Test
    void cannotSavePostWithoutTitle(WebDriver driver, WebDriverWait wait) {
        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "write");

        var contentTextarea = driver.findElement(cssSelector("#content"));
        contentTextarea.sendKeys("Some content");

        var saveDraftBtn = driver.findElement(cssSelector("#saveDraftBtn"));
        saveDraftBtn.click();

        var toast = wait.until(visibilityOfElementLocated(cssSelector("#toast.toast--error")));
        assertThat(toast.getText()).contains("Title is required");
    }

    @Test
    void cannotSavePostWithoutContent(WebDriver driver, WebDriverWait wait) {
        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "write");

        var titleInput = driver.findElement(cssSelector("#title"));
        titleInput.sendKeys("No content");

        var saveDraftBtn = driver.findElement(cssSelector("#saveDraftBtn"));
        saveDraftBtn.click();

        var toast = wait.until(visibilityOfElementLocated(cssSelector("#toast.toast--error")));
        assertThat(toast.getText()).contains("Content is required");
    }

    @Test
    void slugValidationAcceptsValidFormat(WebDriver driver, WebDriverWait wait) {
        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "write");

        var titleInput = driver.findElement(cssSelector("#title"));
        titleInput.sendKeys("Valid Slug Post");
        var slugInput = driver.findElement(cssSelector("#slug"));
        slugInput.clear();
        slugInput.sendKeys("valid-slug-123");

        var contentTextarea = driver.findElement(cssSelector("#content"));
        contentTextarea.sendKeys("Content");

        var publishBtn = driver.findElement(cssSelector("#publishBtn"));
        publishBtn.click();

        var toast = wait.until(visibilityOfElementLocated(cssSelector("#toast.toast--success")));
        assertThat(toast.getText()).contains("Post published!");
    }

    @Test
    void slugValidationRejectsInvalidCharacters(WebDriver driver, WebDriverWait wait) {
        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "write");

        var titleInput = driver.findElement(cssSelector("#title"));
        titleInput.sendKeys("Invalid Slug");
        var slugInput = driver.findElement(cssSelector("#slug"));
        slugInput.clear();
        slugInput.sendKeys("Invalid Slug!"); // uppercase and exclamation

        var contentTextarea = driver.findElement(cssSelector("#content"));
        contentTextarea.sendKeys("Content");

        var publishBtn = driver.findElement(cssSelector("#publishBtn"));
        publishBtn.click();

        var toast = wait.until(visibilityOfElementLocated(cssSelector("#toast.toast--error")));
        assertThat(toast.getText()).contains("Slug can only contain lowercase letters, numbers, and hyphens");
    }

    // ------------------------------------------------------------------------
    // Toolbar functionality (basic)
    // ------------------------------------------------------------------------

    @Test
    void toolbarBoldButtonWrapsTextWithMarkdown(WebDriver driver, WebDriverWait wait) {
        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "write");

        var contentTextarea = wait.until(visibilityOfElementLocated(cssSelector("#content")));
        contentTextarea.sendKeys("some text");
        // Select the word "some"
        contentTextarea.sendKeys(org.openqa.selenium.Keys.HOME);
        contentTextarea.sendKeys(org.openqa.selenium.Keys.chord(org.openqa.selenium.Keys.SHIFT, org.openqa.selenium.Keys.ARROW_RIGHT,
                                                                org.openqa.selenium.Keys.ARROW_RIGHT, org.openqa.selenium.Keys.ARROW_RIGHT,
                                                                org.openqa.selenium.Keys.ARROW_RIGHT));

        var boldBtn = driver.findElement(cssSelector("button[data-command='bold']"));
        boldBtn.click();

        String value = contentTextarea.getAttribute("value");
        assertThat(value).contains("**some** text");
    }
}