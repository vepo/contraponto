package dev.vepo.contraponto.image;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.shared.TestHttp;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResource;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class ImagePickerEndpointTest {

    @TestHTTPResource("/")
    URL baseUrl;

    @Inject
    ImageRepository imageRepository;

    private User owner;
    private dev.vepo.contraponto.blog.Blog blog;
    private Image image;

    @Test
    void guestIsRedirectedFromPicker() {
        given().redirects()
               .follow(false)
               .get("/components/images/picker")
               .then()
               .statusCode(303);
    }

    @Test
    void ownerCanLoadPickerGridFragment() {
        TestHttp.authenticated(owner)
                .get("/components/images/picker/grid")
                .then()
                .statusCode(200)
                .body(containsString("imagePickerGrid"))
                .body(containsString(image.getUuid()));
    }

    @Test
    void ownerCanOpenPickerWithExistingImage() {
        TestHttp.authenticated(owner)
                .get("/components/images/picker")
                .then()
                .statusCode(200)
                .body(containsString("imagePickerModal"))
                .body(containsString(image.getUrl()))
                .body(containsString(image.getUuid()));
    }

    @Test
    void paginationLinksUseNumericPage() {
        for (int i = 0; i < 8; i++) {
            Given.randomCover(blog);
        }
        String expectedNext = "/components/images/picker/grid?page=2";
        TestHttp.authenticated(owner)
                .get("/components/images/picker")
                .then()
                .statusCode(200)
                .body(containsString("hx-get=\"" + expectedNext + "\""));
    }

    @Test
    void paginationPreservesSearchQuery() {
        Given.transaction(() -> {
            var capas = imageRepository.findByUuid(image.getUuid()).orElseThrow();
            capas.setAltText("capas seed");
            imageRepository.update(capas);
        });
        for (int i = 0; i < 8; i++) {
            var extra = Given.randomCover(blog);
            int index = i;
            Given.transaction(() -> {
                var managed = imageRepository.findByUuid(extra.getUuid()).orElseThrow();
                managed.setAltText("capas extra " + index);
                imageRepository.update(managed);
            });
        }
        TestHttp.authenticated(owner)
                .get("/components/images/picker?q=capas")
                .then()
                .statusCode(200)
                .body(containsString("/components/images/picker/grid?page=2"))
                .body(containsString("q=capas"));
    }

    @Test
    void pickerDialogIncludesSearchWithQuery() {
        Given.transaction(() -> {
            var capas = imageRepository.findByUuid(image.getUuid()).orElseThrow();
            capas.setAltText("Capas hero");
            imageRepository.update(capas);
        });
        TestHttp.authenticated(owner)
                .get("/components/images/picker?q=capas")
                .then()
                .statusCode(200)
                .body(containsString("image-library-search"))
                .body(containsString("value=\"capas\""))
                .body(containsString("Capas hero"));
    }

    @Test
    void searchFiltersGridByAltText() {
        var otherImage = Given.randomCover(blog);
        Given.transaction(() -> {
            var capas = imageRepository.findByUuid(image.getUuid()).orElseThrow();
            capas.setAltText("Capas hero");
            imageRepository.update(capas);
            var other = imageRepository.findByUuid(otherImage.getUuid()).orElseThrow();
            other.setAltText("Diagram flow");
            imageRepository.update(other);
        });
        TestHttp.authenticated(owner)
                .get("/components/images/picker/grid?q=capas")
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
                     .withUsername("pickerowner")
                     .withEmail("pickerowner@test.com")
                     .withName("Picker Owner")
                     .withPassword("Password123!")
                     .persist();
        blog = owner.getDefaultBlog();
        image = Given.randomCover(blog);
    }

    @Test
    void strangerPickerDoesNotListOwnersImage() {
        var stranger = Given.user()
                            .withUsername("pickerstranger")
                            .withEmail("pickerstranger@test.com")
                            .withName("Picker Stranger")
                            .withPassword("Password123!")
                            .persist();
        TestHttp.authenticated(stranger)
                .get("/components/images/picker")
                .then()
                .statusCode(200)
                .body(not(containsString(image.getUuid())));
    }
}
