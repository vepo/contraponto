package dev.vepo.contraponto.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

@QuarkusTest
class ImageServiceTest {

    @Inject
    ImageService imageService;

    @Inject
    ImageRepository imageRepository;

    @Inject
    ImageDependencyRepository dependencyRepository;

    private User author;
    private Blog blog;

    @Test
    void deleteImageRejectsReferencedImage() {
        var image = Given.randomCover(blog);
        var post = Given.post()
                        .withAuthor(author)
                        .withBlog(blog)
                        .withTitle("Uses image")
                        .withSlug("uses-image")
                        .withContent("Uses image body")
                        .withPublished(false)
                        .persist();
        Given.transaction(() -> {
            dependencyRepository.persistPostDependency(new PostImageDependency(post, image, ImageRole.INLINE));
        });
        assertThatThrownBy(() -> imageService.deleteImage(image.getUuid()))
                                                                           .isInstanceOf(WebApplicationException.class)
                                                                           .extracting(ex -> ((WebApplicationException) ex).getResponse().getStatus())
                                                                           .isEqualTo(409);
    }

    @Test
    void deleteImageRemovesUnreferencedImage() {
        var image = Given.randomCover(blog);
        imageService.deleteImage(image.getUuid());
        assertThat(imageRepository.findByUuid(image.getUuid())).isEmpty();
    }

    @Test
    void getImageReturnsBytesForStoredFile() throws IOException {
        var image = Given.randomCover(blog);
        var data = imageService.getImage(image.getFilename());
        assertThat(data.contentType()).isEqualTo(image.getContentType());
        assertThat(data.size()).isEqualTo(image.getSize());
        assertThat(data.data()).isNotEmpty();
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        author = Given.user()
                      .withUsername("imgsvc")
                      .withEmail("imgsvc@test.com")
                      .withName("Image Service")
                      .withPassword("Password123!")
                      .persist();
        blog = author.getDefaultBlog();
    }

    @Test
    void uploadImagePersistsMetadataAndFile() throws IOException {
        var file = Given.randomImage();
        try (var stream = new FileInputStream(file.toFile())) {
            var response = imageService.uploadImage(file.getFileName().toString(),
                                                    "image/png",
                                                    stream,
                                                    Files.size(file),
                                                    blog,
                                                    author);
            assertThat(response.id()).isNotBlank();
            assertThat(response.url()).startsWith("/api/images/");
            assertThat(imageRepository.findByUuid(response.id())).isPresent();
            assertThat(Files.exists(imageService.storagePath().resolve(response.filename()))).isTrue();
        }
    }

    @Test
    void uploadImageRejectsInvalidContentType() throws IOException {
        var file = Given.randomImage();
        try (var stream = new FileInputStream(file.toFile())) {
            assertThatThrownBy(() -> imageService.uploadImage(file.getFileName().toString(),
                                                              "text/plain",
                                                              stream,
                                                              Files.size(file),
                                                              blog,
                                                              author))
                                                                      .isInstanceOf(WebApplicationException.class)
                                                                      .extracting(ex -> ((WebApplicationException) ex).getResponse().getStatus())
                                                                      .isEqualTo(400);
        }
    }
}
