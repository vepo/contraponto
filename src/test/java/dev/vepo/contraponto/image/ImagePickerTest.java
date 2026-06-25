package dev.vepo.contraponto.image;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebAuthorTest;
import dev.vepo.contraponto.user.User;

@WebAuthorTest
class ImagePickerTest {

    private User author;
    private Blog blog;
    private Image existingImage;

    @Test
    void selectExistingCoverImageFromPicker(App app) {
        app.login(author)
           .writePage()
           .openCoverImagePicker()
           .selectImageFromPicker(existingImage.getUuid())
           .assertCoverId(existingImage.getUuid())
           .assertCoverPreviewVisible()
           .fillTitle("Post With Picked Cover")
           .fillContent("Body")
           .saveDraft()
           .assertToastSuccess("Draft saved successfully!");
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        author = Given.user()
                      .withUsername("pickerauthor")
                      .withEmail("pickerauthor@test.com")
                      .withName("Picker Author")
                      .withPassword("Password123!")
                      .persist();
        blog = author.getDefaultBlog();
        existingImage = Given.randomCover(blog);
    }
}
