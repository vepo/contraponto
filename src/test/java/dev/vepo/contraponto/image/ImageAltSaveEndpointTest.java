package dev.vepo.contraponto.image;

import static io.restassured.RestAssured.given;
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
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
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
                .put("/forms/blogs/%d/images/%s/alt".formatted(blog.getId(), image.getUuid()))
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
                .put("/forms/blogs/%d/images/%s/alt".formatted(blog.getId(), image.getUuid()))
                .then()
                .statusCode(403);
    }

    @Test
    void unknownImageReturnsNotFound() {
        TestHttp.authenticated(owner)
                .contentType("application/x-www-form-urlencoded")
                .formParam("altText", "Missing")
                .put("/forms/blogs/%d/images/%s/alt".formatted(blog.getId(), "00000000-0000-0000-0000-000000000099"))
                .then()
                .statusCode(404);
    }
}
