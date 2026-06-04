package dev.vepo.contraponto.image;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.shared.TestHttp;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResource;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class ImageControlSearchTest {

    @TestHTTPResource("/")
    URL baseUrl;

    @Inject
    ImageRepository imageRepository;

    private User owner;
    private Blog blog;
    private Image capasImage;
    private Image otherImage;

    @Test
    void searchFiltersByAltText() {
        TestHttp.authenticated(owner)
                .get("/writing/images?q=capas")
                .then()
                .statusCode(200)
                .body(containsString("Capas hero"))
                .body(not(containsString("Diagram flow")));
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        io.restassured.RestAssured.baseURI = baseUrl.getProtocol() + "://" + baseUrl.getHost();
        io.restassured.RestAssured.port = baseUrl.getPort();
        io.restassured.RestAssured.basePath = "";

        owner = Given.user()
                     .withUsername("imgsearch")
                     .withEmail("imgsearch@test.com")
                     .withName("Image Search")
                     .withPassword("Password123!")
                     .persist();
        blog = owner.getDefaultBlog();
        capasImage = Given.randomCover(blog);
        otherImage = Given.randomCover(blog);
        Given.transaction(() -> {
            var capas = imageRepository.findByUuid(capasImage.getUuid()).orElseThrow();
            capas.setAltText("Capas hero");
            imageRepository.update(capas);
            var other = imageRepository.findByUuid(otherImage.getUuid()).orElseThrow();
            other.setAltText("Diagram flow");
            imageRepository.update(other);
        });
    }
}
