package dev.vepo.contraponto.write;

import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebTest;
import dev.vepo.contraponto.user.User;
import jakarta.ws.rs.core.Response.Status;

@WebTest
class WriteTest {

   private static final String TEST_USER_EMAIL = "writer@example.com";
   private static final String TEST_USER_PASSWORD = "writerPass123";
   private static final String TEST_USER_USERNAME = "writeruser";
   private static final String TEST_USER_NAME = "Test Writer";

   private static final String SECOND_USER_EMAIL = "other@example.com";
   private static final String SECOND_USER_PASSWORD = "otherPass123";
   private static final String SECOND_USER_USERNAME = "otheruser";
   private static final String SECOND_USER_NAME = "Other User";

   private User testUser;
   private User secondUser;

   @Test
   void accessingAnotherUsersDraftReturnsNotFound(App app) {
      var otherDraftId = Given.post()
                              .withTitle("Other User Draft")
                              .withContent("Secret")
                              .withAuthor(secondUser)
                              .persist()
                              .getId();

      app.login(testUser)
         .editDraft(otherDraftId)
         // Should show 404 error page
         .assertErrorPage(Status.NOT_FOUND);
   }

   @Test
   void authenticatedUserCanAccessWritePage(App app) {
      app.login(testUser)
         .writePage()
         .fillTitle("My Post")
         .fillContent("Content")
         // Just check the form is present – no assertion needed, the page loads
         .assertTitle("My Post")
         .assertContent("Content");
   }

   @Test
   void cannotPublishPostWithoutContent(App app) {
      app.login(testUser)
         .writePage()
         .fillTitle("No content")
         .publish()
         .assertToastError("Content is required");
   }

   @Test
   void cannotPublishPostWithoutTitle(App app) {
      app.login(testUser)
         .writePage()
         .fillContent("Some content")
         .publish()
         .assertToastError("Title is required");
   }

   @Test
   void cannotSavePostWithoutContent(App app) {
      app.login(testUser)
         .writePage()
         .fillTitle("No content")
         .saveDraft()
         .assertToastError("Content is required");
   }

   @Test
   void cannotSavePostWithoutTitle(App app) {
      app.login(testUser)
         .writePage()
         .fillContent("Some content")
         .saveDraft()
         .assertToastError("Title is required");
   }

   @Test
   void changeCoverImageWhenEditingDraft(App app) throws IOException {
      var firstImage = Given.randomImage();
      var secondImage = Given.randomImage();

      var draftId = app.login(testUser)
                        .writePage()
                        .uploadCover(firstImage)
                        .fillTitle("Post to Change Cover")
                        .fillContent("Content")
                        .saveDraft()
                        .assertToastSuccess("Draft saved successfully!")
                        .getCurrentDraftId();

      app.editDraft(draftId)
         .removeCover()
         .assertCoverPreviewNotVisible()
         .uploadCover(secondImage)
         .assertCoverPreviewVisible()
         .publish()
         .assertToastSuccess("Post published!");

      // Verify new cover appears on the post page
      var slug = "post-to-change-cover";
      app.goToPost(testUser, slug) // we need a method goTo(user, slug) but PostPage will handle it
         .assertCoverImagePresent(); // we need to add this to PostPage

      // Cleanup
      Files.deleteIfExists(firstImage);
      Files.deleteIfExists(secondImage);
   }

   // ... many more tests, but they will follow the same pattern.

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
}