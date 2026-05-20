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
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

@QuarkusIntegrationTest
class ImageServiceTest {

    @Inject
    ImageService imageService;

    @Inject
    ImageRepository imageRepository;

    @Inject
    ImageDependencyRepository dependencyRepository;

    @Inject
    ImageContentRepository imageContentRepository;

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
        assertThatThrownBy(() -> imageService.deleteImage(image.getUuid(), blog.getId()))
                                                                                         .isInstanceOf(WebApplicationException.class)
                                                                                         .extracting(ex -> ((WebApplicationException) ex).getResponse()
                                                                                                                                         .getStatus())
                                                                                         .isEqualTo(409);
    }

    @Test
    void deleteImageRemovesUnreferencedImage() {
        var image = Given.randomCover(blog);
        imageService.deleteImage(image.getUuid(), blog.getId());
        assertThat(imageRepository.findByUuid(image.getUuid())).isEmpty();
    }

    @Test
    void getImageCorrectsLegacySvgStoredAsPng() {
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var image = imageService.storeImportedImage(blog,
                                                    "b2c3d4e5-f6a7-8901-bcde-f12345678901",
                                                    ".svg",
                                                    svg,
                                                    "image/png",
                                                    null);
        assertThat(imageService.getImage(image.getFilename()).contentType()).isEqualTo("image/svg+xml");
    }

    @Test
    void getImageReturnsBytesFromDatabase() {
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
    void storeImportedSvgServesWithSvgContentType() {
        byte[] svg = """
                     <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 10 10">
                       <circle cx="5" cy="5" r="4"/>
                     </svg>
                     """.strip().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var image = imageService.storeImportedImage(blog,
                                                    "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                                    ".svg",
                                                    svg,
                                                    "image/svg+xml",
                                                    null);
        var data = imageService.getImage(image.getFilename());
        assertThat(data.contentType()).isEqualTo("image/svg+xml");
    }

    @Test
    void uploadImagePersistsMetadataAndContent() throws IOException {
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
            var image = imageRepository.findByUuid(response.id()).orElseThrow();
            assertThat(imageContentRepository.findContentByImageId(image.getId())).isPresent();
            assertThat(imageService.getImage(response.filename()).data()).isNotEmpty();
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
