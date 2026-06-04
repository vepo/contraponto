package dev.vepo.contraponto.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.TestHttp;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResource;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class ImageAltSaveEndpointTest {

    @TestHTTPResource("/")
    URL baseUrl;

    @Inject
    ImageRepository imageRepository;

    private User owner;
    private dev.vepo.contraponto.blog.Blog blog;
    private Image image;

    @Test
    void ownerCanSaveAltText() {
        TestHttp.authenticated(owner)
                .contentType("application/x-www-form-urlencoded")
                .formParam("altText", "Accessible caption")
                .formParam("page", "1")
                .put("/forms/images/%s/alt".formatted(image.getUuid()))
                .then()
                .statusCode(200)
                .header("X-Toast-Message", equalTo("Image updated."))
                .body(containsString("image-control"));

        Given.transaction(() -> {
            var updated = imageRepository.findByUuid(image.getUuid());
            assertThat(updated).isPresent();
            assertThat(updated.get().getAltText()).isEqualTo("Accessible caption");
        });
    }

    @Test
    void ownerCanSaveAltTextWithSearchQuery() {
        TestHttp.authenticated(owner)
                .contentType("application/x-www-form-urlencoded")
                .formParam("altText", "Capas cover")
                .formParam("page", "1")
                .formParam("q", "capas")
                .formParam("hub", "writing")
                .put("/forms/images/%s/alt".formatted(image.getUuid()))
                .then()
                .statusCode(200)
                .body(containsString("image-library-search"));
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        io.restassured.RestAssured.baseURI = baseUrl.getProtocol() + "://" + baseUrl.getHost();
        io.restassured.RestAssured.port = baseUrl.getPort();
        io.restassured.RestAssured.basePath = "";

        owner = Given.user()
                     .withUsername("altowner")
                     .withEmail("altowner@test.com")
                     .withName("Alt Owner")
                     .withPassword("Password123!")
                     .persist();
        blog = owner.getDefaultBlog();
        image = Given.randomCover(blog);
        Given.transaction(() -> {
            var managed = imageRepository.findByUuid(image.getUuid()).orElseThrow();
            managed.setAltText("Capas illustration");
            imageRepository.update(managed);
        });
    }

    @Test
    void strangerCannotSaveAltText() {
        var stranger = Given.user()
                            .withUsername("stranger")
                            .withEmail("stranger@test.com")
                            .withName("Stranger")
                            .withPassword("Password123!")
                            .persist();
        TestHttp.authenticated(stranger)
                .contentType("application/x-www-form-urlencoded")
                .formParam("altText", "Hijack")
                .put("/forms/images/%s/alt".formatted(image.getUuid()))
                .then()
                .statusCode(404);
    }

    @Test
    void unknownImageReturnsNotFound() {
        TestHttp.authenticated(owner)
                .contentType("application/x-www-form-urlencoded")
                .formParam("altText", "Missing")
                .put("/forms/images/%s/alt".formatted("00000000-0000-0000-0000-000000000099"))
                .then()
                .statusCode(404);
    }
}
