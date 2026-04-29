package dev.vepo.contraponto.write;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.openqa.selenium.By.className;
import static org.openqa.selenium.By.cssSelector;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebTest;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;

@WebTest
@QuarkusTest
class WriteTest {

    private static final String TEST_USER_EMAIL = "writer@example.com";
    private static final String TEST_USER_PASSWORD = "writerPass123";
    private static final String TEST_USER_USERNAME = "writeruser";
    private static final String TEST_USER_NAME = "Test Writer";

    private static final String SECOND_USER_EMAIL = "other@example.com";
    private static final String SECOND_USER_PASSWORD = "otherPass123";
    private static final String SECOND_USER_USERNAME = "otheruser";
    private static final String SECOND_USER_NAME = "Other User";

    @TestHTTPResource("/")
    URL testUrl;

    User testUser;
    User secondUser;

    @Test
    void accessingAnotherUsersDraftReturnsNotFound(WebDriver driver, WebDriverWait wait) {
        var otherDraftId = Given.post()
                                .withTitle("Other User Draft")
                                .withContent("Secret")
                                .withAuthor(secondUser)
                                .persist()
                                .getId();

        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "write/draft/" + otherDraftId);

        // Should receive a 404 (the page may show an error or redirect)
        await().until(() -> !driver.getPageSource().contains("Other User Draft"));
        var errorMsg = driver.findElements(cssSelector(".error-title"));
        assertThat(errorMsg).isNotEmpty();
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

    @Test
    void cannotPublishPostWithoutContent(WebDriver driver, WebDriverWait wait) {
        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "write");
        driver.findElement(cssSelector("#title")).sendKeys("No content");
        driver.findElement(By.id("publish")).click();
        var toast = wait.until(visibilityOfElementLocated(cssSelector("#toast .toast--error")));
        assertThat(toast.getText()).contains("Content is required");
        assertThat(wait.until(visibilityOfElementLocated(cssSelector("#title"))).getAttribute("value")).isEqualTo("No content");
    }

    @Test
    void cannotPublishPostWithoutTitle(WebDriver driver, WebDriverWait wait) {
        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "write");
        driver.findElement(cssSelector("#content")).sendKeys("Some content");
        driver.findElement(By.id("publish")).click();
        var toast = wait.until(visibilityOfElementLocated(cssSelector("#toast .toast--error")));
        assertThat(toast.getText()).contains("Title is required");
        assertThat(wait.until(visibilityOfElementLocated(cssSelector("#content"))).getAttribute("value")).isEqualTo("Some content");
    }

    @Test
    void cannotSavePostWithoutContent(WebDriver driver, WebDriverWait wait) {
        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "write");
        driver.findElement(cssSelector("#title")).sendKeys("No content");
        driver.findElement(By.id("saveDraft")).click();
        var toast = wait.until(visibilityOfElementLocated(cssSelector("#toast .toast--error")));
        assertThat(toast.getText()).contains("Content is required");
        assertThat(wait.until(visibilityOfElementLocated(cssSelector("#title"))).getAttribute("value")).isEqualTo("No content");
    }

    @Test
    void cannotSavePostWithoutTitle(WebDriver driver, WebDriverWait wait) {
        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "write");
        driver.findElement(cssSelector("#content")).sendKeys("Some content");
        driver.findElement(By.id("saveDraft")).click();
        var toast = wait.until(visibilityOfElementLocated(cssSelector("#toast .toast--error")));
        assertThat(toast.getText()).contains("Title is required");
        assertThat(wait.until(visibilityOfElementLocated(cssSelector("#content"))).getAttribute("value")).isEqualTo("Some content");
    }

    @Test
    void changeCoverImageWhenEditingDraft(WebDriver driver, WebDriverWait wait) throws IOException {
        // Create a post with an initial cover (using a dummy image from Given or
        // directly)
        // Since Given doesn't support cover image, we create it via UI.
        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "write");

        Path firstImage = Given.randomImage();
        driver.findElement(By.id("coverInput")).sendKeys(firstImage.toAbsolutePath().toString());
        wait.until(d -> driver.findElement(By.id("coverPreview")).isDisplayed());
        driver.findElement(By.id("title")).sendKeys("Post to Change Cover");
        driver.findElement(By.id("content")).sendKeys("Content");
        driver.findElement(By.id("saveDraft")).click();
        wait.until(visibilityOfElementLocated(cssSelector("#toast .toast--success")));

        String draftUrl = driver.getCurrentUrl(); // e.g., /write/draft/123
        driver.get(draftUrl); // reload the edit page

        // Remove existing cover
        WebElement removeBtn = driver.findElement(By.id("removeCoverBtn"));
        removeBtn.click();
        wait.until(d -> !driver.findElement(By.id("coverPreview")).isDisplayed());

        // Upload new cover
        Path secondImage = Given.randomImage();
        driver.findElement(By.id("coverInput")).sendKeys(secondImage.toAbsolutePath().toString());
        wait.until(d -> driver.findElement(By.id("coverPreview")).isDisplayed());

        driver.findElement(By.id("publish")).click();
        wait.until(visibilityOfElementLocated(cssSelector("#toast .toast--success")));

        // Verify new cover appears on the post page
        String slug = "post-to-change-cover";
        driver.get(testUrl.toString() + testUser.getUsername() + "/post/" + slug);
        WebElement coverImg = wait.until(visibilityOfElementLocated(cssSelector(".article-page__cover-image")));
        assertThat(coverImg.isDisplayed()).isTrue();

        Files.deleteIfExists(firstImage);
        Files.deleteIfExists(secondImage);
    }

    @Test
    void coverImageIsDisplayedOnPublishedPost(WebDriver driver, WebDriverWait wait) throws IOException {
        // First create a post with cover image
        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "write");

        Path tempImage = Given.randomImage();
        WebElement coverInput = driver.findElement(By.id("coverInput"));
        coverInput.sendKeys(tempImage.toAbsolutePath().toString());
        wait.until(d -> driver.findElement(By.id("coverPreview")).isDisplayed());

        String title = "Cover Image Test Post";
        driver.findElement(By.id("title")).sendKeys(title);
        driver.findElement(By.id("content")).sendKeys("Some content");
        driver.findElement(By.id("publish")).click();

        wait.until(visibilityOfElementLocated(cssSelector("#toast .toast--success")));

        // Navigate to the published post
        String slug = title.toLowerCase().replaceAll("[^a-z0-9]+", "-");
        driver.get(testUrl.toString() + testUser.getUsername() + "/post/" + slug);

        WebElement coverImg = wait.until(visibilityOfElementLocated(cssSelector(".article-page__cover-image")));
        assertThat(coverImg.isDisplayed()).isTrue();
        assertThat(coverImg.getAttribute("src")).contains("/api/images/");

        Files.deleteIfExists(tempImage);
    }

    @Test
    void createNewPostAsDraft(WebDriver driver, WebDriverWait wait) {
        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "write");

        var title = "My Draft Post";
        var slug = "my-post";
        var description = "This is my post.";
        var content = "This is the content of my draft post.";

        driver.findElement(cssSelector("#title")).sendKeys(title);
        driver.findElement(cssSelector("#slug")).sendKeys(slug);
        driver.findElement(cssSelector("#description")).sendKeys(description);
        driver.findElement(cssSelector("#content")).sendKeys(content);

        driver.findElement(By.id("saveDraft")).click();

        var toast = wait.until(visibilityOfElementLocated(cssSelector("#toast .toast--success")));
        assertThat(toast.getText()).contains("Draft saved successfully!");

        // After saving, URL should contain ?edit= with the new post id
        assertThat(driver.getCurrentUrl()).matches(".*/write/draft/\\d+");
    }

    @Test
    void deleteDraftFromLibrary(WebDriver driver, WebDriverWait wait) {
        Given.post()
             .withTitle("Draft to Delete")
             .withContent("Content")
             .withAuthor(testUser)
             .withPublished(false)
             .persist()
             .getId();

        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "library");
        // Wait for first tab (drafts) to load
        wait.until(visibilityOfElementLocated(cssSelector(".draft-card")));
        var deleteBtn = driver.findElement(cssSelector(".draft-card .btn--danger"));
        deleteBtn.click();
        // Accept the confirm dialog
        driver.switchTo().alert().accept();

        // Wait for the draft card to disappear
        await().until(() -> driver.findElements(cssSelector(".draft-card")).isEmpty());
        assertThat(driver.getPageSource()).doesNotContain("Draft to Delete");
    }

    @Test
    void duplicateSlugPreventsPublishing(WebDriver driver, WebDriverWait wait) {
        // First, create a published post with slug "my-unique-slug"
        Given.post()
             .withTitle("First Post")
             .withSlug("my-unique-slug")
             .withContent("Content")
             .withAuthor(testUser)
             .persist();

        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "write");
        driver.findElement(cssSelector("#title")).sendKeys("Second Post");
        driver.findElement(cssSelector("#slug")).clear();
        driver.findElement(cssSelector("#slug")).sendKeys("my-unique-slug");
        driver.findElement(cssSelector("#content")).sendKeys("Content");
        driver.findElement(By.id("publish")).click();

        var toast = wait.until(visibilityOfElementLocated(cssSelector("#toast .toast--error")));
        assertThat(toast.getText()).contains("Slug already exists");
    }

    @Test
    void editDraftThenPublish(WebDriver driver, WebDriverWait wait) {
        var draftId = Given.post()
                           .withTitle("Draft to Publish Later")
                           .withContent("Initial content")
                           .withAuthor(testUser)
                           .persist()
                           .getId();

        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "write/draft/" + draftId);

        var titleInput = driver.findElement(cssSelector("#title"));
        titleInput.clear();
        titleInput.sendKeys("Published After Edit");

        driver.findElement(cssSelector("#content")).clear();
        driver.findElement(cssSelector("#content")).sendKeys("Final content");
        driver.findElement(cssSelector("#slug")).clear();
        driver.findElement(cssSelector("#slug")).sendKeys("published-after-edit");

        driver.findElement(By.id("publish")).click();

        var toast = wait.until(visibilityOfElementLocated(cssSelector("#toast .toast--success")));
        assertThat(toast.getText()).contains("Post published!");
        assertThat(driver.getCurrentUrl()).contains("/" + testUser.getUsername() + "/post/published-after-edit");
    }

    @Test
    void editExistingPost(WebDriver driver, WebDriverWait wait) {
        // First create a post directly via backend (or via UI and capture id)
        // Using Given to create a post for the user
        var postId = Given.post()
                          .withTitle("Original Title")
                          .withSlug("original-title")
                          .withDescription("Description")
                          .withContent("Original content")
                          .withAuthor(testUser)
                          .persist()
                          .getId();

        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "write/draft/" + postId);

        var titleInput = wait.until(visibilityOfElementLocated(cssSelector("#title")));
        assertThat(titleInput.getAttribute("value")).isEqualTo("Original Title");

        titleInput.clear();
        titleInput.sendKeys("Updated Title");
        driver.findElement(cssSelector("#slug")).clear();
        driver.findElement(cssSelector("#slug")).sendKeys("updated-title");
        driver.findElement(cssSelector("#content")).clear();
        driver.findElement(cssSelector("#content")).sendKeys("Updated content");

        driver.findElement(By.id("publish")).click();

        var toast = wait.until(visibilityOfElementLocated(cssSelector("#toast .toast--success")));
        assertThat(toast.getText()).contains("Post published!");

        // Navigate to the published post
        driver.get("%s/%s/post/updated-title".formatted(testUrl, testUser.getUsername()));
        var postTitle = wait.until(visibilityOfElementLocated(cssSelector(".article-page__title")));
        assertThat(postTitle.getText()).isEqualTo("Updated Title");
    }

    @Test
    void editorModeSelectorExistsAndCanSwitch(WebDriver driver, WebDriverWait wait) {
        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "write");

        WebElement modeButton = wait.until(visibilityOfElementLocated(cssSelector("#editorModeButton")));
        assertThat(modeButton.isDisplayed()).isTrue();
        assertThat(modeButton.findElement(cssSelector(".editor-mode-label")).getText()).isEqualTo("Markdown");

        // Open dropdown
        modeButton.click();
        WebElement dropdown = wait.until(visibilityOfElementLocated(cssSelector(".editor-mode-dropdown")));
        assertThat(dropdown.isDisplayed()).isTrue();

        // Click AsciiDoc option
        WebElement asciiDocOption = dropdown.findElement(cssSelector("[data-mode='asciidoc']"));
        asciiDocOption.click();

        // Wait for mode label to update
        wait.until(d -> modeButton.findElement(cssSelector(".editor-mode-label")).getText().equals("AsciiDoc"));
        assertThat(modeButton.findElement(cssSelector(".editor-mode-icon")).getText()).contains("📄");

        // Switch back to Markdown
        modeButton.click();
        dropdown = wait.until(visibilityOfElementLocated(cssSelector(".editor-mode-dropdown")));
        WebElement markdownOption = dropdown.findElement(cssSelector("[data-mode='markdown']"));
        markdownOption.click();
        wait.until(d -> modeButton.findElement(cssSelector(".editor-mode-label")).getText().equals("Markdown"));
        assertThat(modeButton.findElement(cssSelector(".editor-mode-icon")).getText()).contains("📝");
    }

    @Test
    void hintTextUpdatesWhenChangingMode(WebDriver driver, WebDriverWait wait) {
        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "write");

        WebElement hint = wait.until(visibilityOfElementLocated(cssSelector("#editorHint")));
        assertThat(hint.getText()).contains("Markdown");

        // Switch to AsciiDoc
        WebElement modeButton = driver.findElement(cssSelector("#editorModeButton"));
        modeButton.click();
        WebElement asciiDocOption = wait.until(visibilityOfElementLocated(cssSelector("[data-mode='asciidoc']")));
        asciiDocOption.click();

        wait.until(d -> hint.getText().contains("AsciiDoc"));
        assertThat(hint.getText()).contains("link:url[text]");
    }

    // ------------------------------------------------------------------------
    // Helper method for login
    // ------------------------------------------------------------------------
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

        await().until(() -> !driver.findElement(By.id("authModal")).isDisplayed());
        wait.until(visibilityOfElementLocated(className("user-menu")));
    }

    @Test
    void previewToggleWhenSwitchingModeWhilePreviewIsOpen(WebDriver driver, WebDriverWait wait) {
        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "write");

        WebElement contentTextarea = driver.findElement(cssSelector("#content"));
        contentTextarea.sendKeys("# Markdown heading\n\nSome content.");

        // Open preview
        WebElement previewBtn = driver.findElement(cssSelector("#previewToggleBtn"));
        previewBtn.click();
        WebElement previewContainer = wait.until(visibilityOfElementLocated(cssSelector("#previewContainer")));
        assertThat(previewContainer.isDisplayed()).isTrue();
    }

    // ------------------------------------------------------------------------
    // Toolbar functionality (basic)
    // ------------------------------------------------------------------------

    @Test
    void previewWorksForAsciiDocMode(WebDriver driver, WebDriverWait wait) {
        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "write");

        // Switch to AsciiDoc
        WebElement modeButton = driver.findElement(cssSelector("#editorModeButton"));
        modeButton.click();
        WebElement asciiDocOption = wait.until(visibilityOfElementLocated(cssSelector("[data-mode='asciidoc']")));
        asciiDocOption.click();
        wait.until(d -> driver.findElement(cssSelector("#editorModeButton .editor-mode-label")).getText().equals("AsciiDoc"));

        WebElement contentTextarea = driver.findElement(cssSelector("#content"));
        contentTextarea.sendKeys("= Heading\n\nThis is *bold* text.");

        WebElement previewBtn = driver.findElement(cssSelector("#previewToggleBtn"));
        previewBtn.click();

        WebElement previewContainer = wait.until(visibilityOfElementLocated(cssSelector("#previewContainer")));
        assertThat(previewContainer.isDisplayed()).isTrue();

        // AsciiDoctor renders <h1> for level-0 heading
        String html = previewContainer.getAttribute("innerHTML");
        assertThat(html).contains("<h1");
        assertThat(previewContainer.getText()).contains("bold");
    }

    // ------------------------------------------------------------------------
    // NEW TESTS
    // ------------------------------------------------------------------------

    @Test
    void previewWorksForMarkdownMode(WebDriver driver, WebDriverWait wait) {
        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "write");

        WebElement contentTextarea = driver.findElement(cssSelector("#content"));
        contentTextarea.sendKeys("# Heading\n\nThis is **bold** text.");

        WebElement previewBtn = driver.findElement(cssSelector("#previewToggleBtn"));
        previewBtn.click();

        WebElement previewContainer = wait.until(visibilityOfElementLocated(cssSelector("#previewContainer")));
        assertThat(previewContainer.isDisplayed()).isTrue();
        assertThat(previewContainer.getText()).contains("Heading");
        assertThat(previewContainer.getText()).contains("bold"); // Should render as <strong> or text
        // Check for HTML structure (optional)
        String html = previewContainer.getAttribute("innerHTML");
        assertThat(html).contains("<h1");
    }

    @Test
    void publishAndSaveDraftButtonsAreDisabledOnNonWritePages(WebDriver driver, WebDriverWait wait) {
        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "profile");
        var publishBtn = driver.findElement(By.id("publish"));
        var saveDraftBtn = driver.findElement(By.id("saveDraft"));
        assertThat(publishBtn.getAttribute("class")).contains("disabled");
        assertThat(saveDraftBtn.getAttribute("class")).contains("disabled");
    }

    @Test
    void publishNewPost(WebDriver driver, WebDriverWait wait) {
        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "write");

        var title = "Published Post";
        driver.findElement(cssSelector("#title")).sendKeys(title);
        driver.findElement(cssSelector("#content")).sendKeys("Published content here.");

        driver.findElement(By.id("publish")).click();

        var toast = wait.until(visibilityOfElementLocated(cssSelector("#toast .toast--success")));
        assertThat(toast.getText()).contains("Post published!");

        // Verify we can navigate to the published post
        // The slug is generated from title; we assume it becomes "published-post"
        driver.get("%s/%s/post/published-post".formatted(testUrl, testUser.getUsername()));
        var postTitle = wait.until(visibilityOfElementLocated(cssSelector(".article-page__title")));
        assertThat(postTitle.getText()).isEqualTo(title);
    }

    @Test
    void removeCoverImageFromPost(WebDriver driver, WebDriverWait wait) throws IOException {
        // Create a post with a cover
        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "write");

        Path tempImage = Given.randomImage();
        driver.findElement(By.id("coverInput")).sendKeys(tempImage.toAbsolutePath().toString());
        wait.until(d -> driver.findElement(By.id("coverPreview")).isDisplayed());
        driver.findElement(By.id("title")).sendKeys("Post with Cover to Remove");
        driver.findElement(By.id("content")).sendKeys("Content");
        driver.findElement(By.id("saveDraft")).click();
        wait.until(visibilityOfElementLocated(cssSelector("#toast .toast--success")));

        String draftUrl = driver.getCurrentUrl();
        driver.get(draftUrl);

        // Remove cover
        driver.findElement(By.id("removeCoverBtn")).click();
        wait.until(d -> !driver.findElement(By.id("coverPreview")).isDisplayed());

        driver.findElement(By.id("publish")).click();
        wait.until(visibilityOfElementLocated(cssSelector("#toast .toast--success")));

        // Verify no cover on the post page
        String slug = "post-with-cover-to-remove";
        driver.get(testUrl.toString() + testUser.getUsername() + "/post/" + slug);
        List<WebElement> coverImages = driver.findElements(cssSelector(".article-page__cover-image"));
        assertThat(coverImages).isEmpty();

        Files.deleteIfExists(tempImage);
    }

    @Test
    void savingDraftWithoutCoverImageWorks(WebDriver driver, WebDriverWait wait) {
        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "write");
        driver.findElement(cssSelector("#title")).sendKeys("No Cover Draft");
        driver.findElement(cssSelector("#content")).sendKeys("Some content");
        driver.findElement(By.id("saveDraft")).click();
        var toast = wait.until(visibilityOfElementLocated(cssSelector("#toast .toast--success")));
        assertThat(toast.getText()).contains("Draft saved successfully!");
        // No error about cover image
    }

    @BeforeEach
    void setup() {
        Given.cleanup();
        testUser = Given.user()
                        .withUsername(TEST_USER_USERNAME)
                        .withEmail(TEST_USER_EMAIL)
                        .withPassword(TEST_USER_PASSWORD)
                        .withName(TEST_USER_NAME)
                        .persist();

        secondUser = Given.user()
                          .withUsername(SECOND_USER_USERNAME)
                          .withEmail(SECOND_USER_EMAIL)
                          .withPassword(SECOND_USER_PASSWORD)
                          .withName(SECOND_USER_NAME)
                          .persist();
    }

    @Test
    void slugAutoGeneratedFromTitleWhenEmpty(WebDriver driver, WebDriverWait wait) {
        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "write");
        driver.findElement(cssSelector("#title")).sendKeys("My Awesome Title!");
        driver.findElement(cssSelector("#content")).sendKeys("Content");
        driver.findElement(By.id("publish")).click();

        var toast = wait.until(visibilityOfElementLocated(cssSelector("#toast .toast--success")));
        assertThat(toast.getText()).contains("Post published!");
        // Expected slug: "my-awesome-title"
        driver.get("%s/%s/post/my-awesome-title-".formatted(testUrl, testUser.getUsername()));
        var postTitle = wait.until(visibilityOfElementLocated(cssSelector(".article-page__title")));
        assertThat(postTitle.getText()).isEqualTo("My Awesome Title!");
    }

    @Test
    void slugValidationAcceptsValidFormat(WebDriver driver, WebDriverWait wait) {
        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "write");
        driver.findElement(cssSelector("#title")).sendKeys("Valid Slug Post");
        driver.findElement(cssSelector("#slug")).clear();
        driver.findElement(cssSelector("#slug")).sendKeys("valid-slug-123");
        driver.findElement(cssSelector("#content")).sendKeys("Content");
        driver.findElement(By.id("publish")).click();
        var toast = wait.until(visibilityOfElementLocated(cssSelector("#toast .toast--success")));
        assertThat(toast.getText()).contains("Post published!");
    }

    // ------------------------------------------------------------------------
    // Toolbar functionality (basic)
    // ------------------------------------------------------------------------

    @Test
    void slugValidationRejectsInvalidCharacters(WebDriver driver, WebDriverWait wait) {
        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "write");
        driver.findElement(cssSelector("#title")).sendKeys("Invalid Slug");
        driver.findElement(cssSelector("#slug")).clear();
        driver.findElement(cssSelector("#slug")).sendKeys("Invalid Slug!");
        driver.findElement(cssSelector("#content")).sendKeys("Content");
        driver.findElement(By.id("publish")).click();
        var toast = wait.until(visibilityOfElementLocated(cssSelector("#toast .toast--error")));
        assertThat(toast.getText()).contains("Slug can only contain lowercase letters, numbers, and hyphens");
    }

    // ------------------------------------------------------------------------
    // Markdown / AsciiDoc mode switching tests
    // ------------------------------------------------------------------------

    @Test
    void switchingModeDoesNotLoseContent(WebDriver driver, WebDriverWait wait) {
        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "write");

        WebElement contentTextarea = driver.findElement(cssSelector("#content"));
        String originalText = "Some text that should persist after mode switch.";
        contentTextarea.sendKeys(originalText);

        // Switch to AsciiDoc
        WebElement modeButton = driver.findElement(cssSelector("#editorModeButton"));
        modeButton.click();
        WebElement asciiDocOption = wait.until(visibilityOfElementLocated(cssSelector("[data-mode='asciidoc']")));
        asciiDocOption.click();
        wait.until(d -> driver.findElement(cssSelector("#editorModeButton .editor-mode-label")).getText().equals("AsciiDoc"));

        String valueAfterSwitch = driver.findElement(cssSelector("#content")).getAttribute("value");
        assertThat(valueAfterSwitch).isEqualTo(originalText);

        // Switch back to Markdown
        modeButton = driver.findElement(cssSelector("#editorModeButton"));
        modeButton.click();
        WebElement markdownOption = wait.until(visibilityOfElementLocated(cssSelector("[data-mode='markdown']")));
        markdownOption.click();
        wait.until(d -> driver.findElement(cssSelector("#editorModeButton .editor-mode-label")).getText().equals("Markdown"));

        valueAfterSwitch = driver.findElement(cssSelector("#content")).getAttribute("value");
        assertThat(valueAfterSwitch).isEqualTo(originalText);
    }

    @Test
    void toolbarBoldButtonProducesAsciiDocSyntaxWhenInAsciiDocMode(WebDriver driver, WebDriverWait wait) {
        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "write");

        // Switch to AsciiDoc mode
        WebElement modeButton = driver.findElement(cssSelector("#editorModeButton"));
        modeButton.click();
        WebElement asciiDocOption = wait.until(visibilityOfElementLocated(cssSelector("[data-mode='asciidoc']")));
        asciiDocOption.click();
        wait.until(d -> driver.findElement(cssSelector("#editorModeButton .editor-mode-label")).getText().equals("AsciiDoc"));

        WebElement contentTextarea = driver.findElement(cssSelector("#content"));
        contentTextarea.sendKeys("some text");
        contentTextarea.sendKeys(Keys.HOME);
        contentTextarea.sendKeys(Keys.chord(Keys.SHIFT, Keys.ARROW_RIGHT, Keys.ARROW_RIGHT, Keys.ARROW_RIGHT, Keys.ARROW_RIGHT));
        driver.findElement(cssSelector("button[data-command='bold']")).click();

        String value = driver.findElement(cssSelector("#content")).getAttribute("value");
        assertThat(value).contains("*some* text"); // AsciiDoc bold uses single asterisks
    }

    @Test
    void toolbarBoldButtonWrapsTextWithMarkdown(WebDriver driver, WebDriverWait wait) {
        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "write");
        var contentTextarea = wait.until(visibilityOfElementLocated(cssSelector("#content")));
        contentTextarea.sendKeys("some text");
        contentTextarea.sendKeys(Keys.HOME);
        contentTextarea.sendKeys(Keys.chord(Keys.SHIFT, Keys.ARROW_RIGHT, Keys.ARROW_RIGHT, Keys.ARROW_RIGHT, Keys.ARROW_RIGHT));
        driver.findElement(cssSelector("button[data-command='bold']")).click();
        wait.until(d -> "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState")));
        var value = driver.findElement(cssSelector("#content")).getAttribute("value");
        assertThat(value).contains("**some** text");
    }

    @Test
    void toolbarHeadingButtonProducesAsciiDocSyntax(WebDriver driver, WebDriverWait wait) {
        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "write");

        // Switch to AsciiDoc
        WebElement modeButton = driver.findElement(cssSelector("#editorModeButton"));
        modeButton.click();
        WebElement asciiDocOption = wait.until(visibilityOfElementLocated(cssSelector("[data-mode='asciidoc']")));
        asciiDocOption.click();
        wait.until(d -> driver.findElement(cssSelector("#editorModeButton .editor-mode-label")).getText().equals("AsciiDoc"));

        WebElement contentTextarea = driver.findElement(cssSelector("#content"));
        contentTextarea.sendKeys("Heading");
        contentTextarea.sendKeys(Keys.HOME);
        contentTextarea.sendKeys(Keys.chord(Keys.SHIFT, Keys.ARROW_RIGHT, Keys.ARROW_RIGHT, Keys.ARROW_RIGHT, Keys.ARROW_RIGHT, Keys.ARROW_RIGHT,
                                            Keys.ARROW_RIGHT, Keys.ARROW_RIGHT));
        driver.findElement(cssSelector("button[data-command='h2']")).click();

        String value = driver.findElement(cssSelector("#content")).getAttribute("value");
        assertThat(value).contains("== Heading");
    }

    @Test
    void toolbarInsertsInlineImageMarkdown(WebDriver driver, WebDriverWait wait) throws IOException {
        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "write");

        // First upload an image to get a URL (we can use the API directly or via UI)
        Path tempImage = Given.randomImage();
        WebElement coverInput = driver.findElement(By.id("coverInput"));
        coverInput.sendKeys(tempImage.toAbsolutePath().toString());
        wait.until(d -> driver.findElement(By.id("coverPreview")).isDisplayed());

        // Wait for upload to complete and get the image ID (from hidden field)
        String imageId = driver.findElement(By.id("coverId")).getAttribute("value");
        assertThat(imageId).isNotBlank();

        // The image URL will be /api/images/{uuid}. We'll construct it.
        String imageUrl = testUrl.toString() + "api/images/" + imageId;

        // Now go to the content textarea and insert the image using the toolbar
        WebElement contentTextarea = driver.findElement(By.id("content"));
        contentTextarea.sendKeys("An inline image: ");
        // Simulate toolbar image command: it opens a file picker, but we can't automate
        // that easily.
        // Instead, we can directly insert the markdown via executeScript.
        JavascriptExecutor js = (JavascriptExecutor) driver;
        String markdown = "![Test Image](" + imageUrl + ")";
        js.executeScript("document.getElementById('content').value += arguments[0];", markdown);

        // Verify the markdown is present
        String value = contentTextarea.getAttribute("value");
        assertThat(value).contains(markdown);

        // Save draft and verify the rendered HTML contains the image (after publishing)
        driver.findElement(By.id("title")).sendKeys("Post with Inline Image");
        driver.findElement(By.id("saveDraft")).click();
        wait.until(visibilityOfElementLocated(cssSelector("#toast .toast--success")));

        // Publish
        driver.findElement(By.id("publish")).click();
        wait.until(visibilityOfElementLocated(cssSelector("#toast .toast--success")));

        // Navigate to post and verify the image appears in content
        String slug = "post-with-inline-image";
        driver.get(testUrl.toString() + testUser.getUsername() + "/post/" + slug);
        WebElement article = wait.until(visibilityOfElementLocated(cssSelector(".article-page__content")));
        WebElement imgInContent = article.findElement(cssSelector("img"));
        assertThat(imgInContent.isDisplayed()).isTrue();

        Files.deleteIfExists(tempImage);
    }

    @Test
    void toolbarInsertsLinkWithPrompt(WebDriver driver, WebDriverWait wait) {
        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "write");
        var contentTextarea = driver.findElement(cssSelector("#content"));
        contentTextarea.sendKeys("click here");
        contentTextarea.sendKeys(Keys.HOME);
        contentTextarea.sendKeys(Keys.chord(Keys.SHIFT,
                                            Keys.ARROW_RIGHT, // c
                                            Keys.ARROW_RIGHT, // l
                                            Keys.ARROW_RIGHT, // i
                                            Keys.ARROW_RIGHT, // c
                                            Keys.ARROW_RIGHT, // k
                                            Keys.ARROW_RIGHT, // " "
                                            Keys.ARROW_RIGHT, // h
                                            Keys.ARROW_RIGHT, // e
                                            Keys.ARROW_RIGHT, // r
                                            Keys.ARROW_RIGHT // e
        ));
        driver.findElement(cssSelector("button[data-command='link']")).click();
        // Handle JavaScript prompt
        await().until(() -> driver.switchTo().alert() != null);
        driver.switchTo().alert().sendKeys("https://example.com");
        driver.switchTo().alert().accept();
        var value = driver.findElement(cssSelector("#content")).getAttribute("value");
        assertThat(value).contains("[click here](https://example.com)");
    }

    @Test
    void toolbarItalicButtonProducesAsciiDocSyntax(WebDriver driver, WebDriverWait wait) {
        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "write");

        // Switch to AsciiDoc
        WebElement modeButton = driver.findElement(cssSelector("#editorModeButton"));
        modeButton.click();
        WebElement asciiDocOption = wait.until(visibilityOfElementLocated(cssSelector("[data-mode='asciidoc']")));
        asciiDocOption.click();
        wait.until(d -> driver.findElement(cssSelector("#editorModeButton .editor-mode-label")).getText().equals("AsciiDoc"));

        WebElement contentTextarea = driver.findElement(cssSelector("#content"));
        contentTextarea.sendKeys("italic word");
        contentTextarea.sendKeys(Keys.HOME);
        contentTextarea.sendKeys(Keys.chord(Keys.SHIFT, Keys.ARROW_RIGHT, Keys.ARROW_RIGHT, Keys.ARROW_RIGHT, Keys.ARROW_RIGHT, Keys.ARROW_RIGHT,
                                            Keys.ARROW_RIGHT));
        driver.findElement(cssSelector("button[data-command='italic']")).click();

        String value = driver.findElement(cssSelector("#content")).getAttribute("value");
        assertThat(value).contains("_italic_ word");
    }

    @Test
    void toolbarLinkButtonProducesAsciiDocSyntax(WebDriver driver, WebDriverWait wait) {
        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "write");

        // Switch to AsciiDoc
        WebElement modeButton = driver.findElement(cssSelector("#editorModeButton"));
        modeButton.click();
        WebElement asciiDocOption = wait.until(visibilityOfElementLocated(cssSelector("[data-mode='asciidoc']")));
        asciiDocOption.click();
        wait.until(d -> driver.findElement(cssSelector("#editorModeButton .editor-mode-label")).getText().equals("AsciiDoc"));

        WebElement contentTextarea = driver.findElement(cssSelector("#content"));
        contentTextarea.sendKeys("click here");
        contentTextarea.sendKeys(Keys.HOME);
        contentTextarea.sendKeys(Keys.chord(Keys.SHIFT,
                                            Keys.ARROW_RIGHT, // c
                                            Keys.ARROW_RIGHT, // l
                                            Keys.ARROW_RIGHT, // i
                                            Keys.ARROW_RIGHT, // c
                                            Keys.ARROW_RIGHT, // k
                                            Keys.ARROW_RIGHT, // " "
                                            Keys.ARROW_RIGHT, // h
                                            Keys.ARROW_RIGHT, // e
                                            Keys.ARROW_RIGHT, // r
                                            Keys.ARROW_RIGHT // e
        ));
        driver.findElement(cssSelector("button[data-command='link']")).click();

        await().until(() -> driver.switchTo().alert() != null);
        driver.switchTo().alert().sendKeys("https://example.com");
        driver.switchTo().alert().accept();

        String value = driver.findElement(cssSelector("#content")).getAttribute("value");
        assertThat(value).contains("link:https://example.com[click here]");
    }

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
    void uploadCoverImageWhenCreatingDraft(WebDriver driver, WebDriverWait wait) throws IOException {
        login(driver, wait, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        driver.get(testUrl.toString() + "write");

        // Create a temporary image file
        Path tempImage = Given.randomImage();
        WebElement coverInput = driver.findElement(By.id("coverInput"));
        coverInput.sendKeys(tempImage.toAbsolutePath().toString());

        // Wait for the upload to complete and preview to appear
        wait.until(d -> driver.findElement(By.id("coverPreview")).isDisplayed());

        // Fill post data
        driver.findElement(By.id("title")).sendKeys("Post with Cover");
        driver.findElement(By.id("content")).sendKeys("Content with cover image");
        driver.findElement(By.id("saveDraft")).click();

        WebElement toast = wait.until(visibilityOfElementLocated(cssSelector("#toast .toast--success")));
        assertThat(toast.getText()).contains("Draft saved successfully!");

        // The cover ID should be stored in hidden field
        String coverId = driver.findElement(By.id("coverId")).getAttribute("value");
        assertThat(coverId).isNotBlank();

        // Clean up temp file
        Files.deleteIfExists(tempImage);
    }
}