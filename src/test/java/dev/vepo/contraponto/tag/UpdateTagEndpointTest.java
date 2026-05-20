package dev.vepo.contraponto.tag;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.TestHttp;
import dev.vepo.contraponto.user.Role;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResource;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;

@QuarkusIntegrationTest
class UpdateTagEndpointTest {

    /**
     * Local helper so we do not add a static RestAssured import that formatter
     * tools may mishandle.
     */
    private static final class RestAssuredPort {
        static void configure(URL baseUrl) {
            io.restassured.RestAssured.baseURI = baseUrl.getProtocol() + "://" + baseUrl.getHost();
            io.restassured.RestAssured.port = baseUrl.getPort();
            io.restassured.RestAssured.basePath = "";
        }

        private RestAssuredPort() {}
    }

    private static final String ERROR_NAME = "Tag name is required.";
    private static final String ERROR_SLUG = "Tag URL slug is invalid. Use lowercase letters, numbers, and hyphens only.";
    private static final String ERROR_SLUG_TAKEN = "That slug is already used by another tag.";

    private static final String SUCCESS_MESSAGE = "Tag updated.";

    private static io.restassured.specification.RequestSpecification authorized(User user) {
        return TestHttp.authenticated(user);
    }

    @TestHTTPResource("/")
    URL baseUrl;
    private User editor;

    private User reader;

    private long newsTagId;

    @Test
    void blank_name_returns_error_toast() {
        authorized(editor).contentType("application/x-www-form-urlencoded")
                          .formParam("tagId", newsTagId)
                          .formParam("name", " ")
                          .formParam("slug", "news")
                          .post("/forms/tags/update")
                          .then()
                          .statusCode(400)
                          .header("X-Toast-Message", equalTo(ERROR_NAME));
    }

    @Test
    void blank_slug_returns_error_toast() {
        authorized(editor).contentType("application/x-www-form-urlencoded")
                          .formParam("tagId", newsTagId)
                          .formParam("name", "News")
                          .formParam("slug", " ")
                          .post("/forms/tags/update")
                          .then()
                          .statusCode(400)
                          .header("X-Toast-Message", equalTo(ERROR_SLUG));
    }

    @Test
    void clears_description_when_blank() {
        Given.transaction(() -> {
            Tag t = Given.inject(TagRepository.class).findById(newsTagId).orElseThrow();
            t.setDescription("will clear");
            Given.inject(TagRepository.class).save(t);
        });

        authorized(editor).contentType("application/x-www-form-urlencoded")
                          .formParam("tagId", newsTagId)
                          .formParam("name", "News")
                          .formParam("slug", "news")
                          .formParam("description", "   ")
                          .post("/forms/tags/update")
                          .then()
                          .statusCode(200);

        Tag updated = Given.transaction(() -> Given.inject(TagRepository.class)
                                                   .findById(newsTagId)
                                                   .orElseThrow());
        assertThat(updated.getDescription()).isNull();
    }

    @Test
    void duplicate_slug_returns_error_toast() {
        authorized(editor).contentType("application/x-www-form-urlencoded")
                          .formParam("tagId", newsTagId)
                          .formParam("name", "News")
                          .formParam("slug", "other")
                          .post("/forms/tags/update")
                          .then()
                          .statusCode(400)
                          .header("X-Toast-Message", equalTo(ERROR_SLUG_TAKEN));

        Tag news = Given.transaction(() -> Given.inject(TagRepository.class)
                                                .findById(newsTagId)
                                                .orElseThrow());
        assertThat(news.getSlug()).isEqualTo("news");
    }

    @Test
    void editor_updates_name_slug_and_returns_success_toast() {
        authorized(editor).contentType("application/x-www-form-urlencoded")
                          .formParam("tagId", newsTagId)
                          .formParam("name", "Headlines ")
                          .formParam("slug", " bulletin ")
                          .formParam("description", "  tag blurb ")
                          .post("/forms/tags/update")
                          .then()
                          .statusCode(200)
                          .header("X-Toast-Message", equalTo(SUCCESS_MESSAGE))
                          .header("X-Toast-Type", equalTo("Success"))
                          .header("HX-Push-Url", equalTo("/tags/bulletin"));

        Tag updated = Given.transaction(() -> Given.inject(TagRepository.class)
                                                   .findBySlug("bulletin")
                                                   .orElseThrow());
        assertThat(updated.getName()).isEqualTo("Headlines");
        assertThat(updated.getSlug()).isEqualTo("bulletin");
        assertThat(updated.getDescription()).isEqualTo("tag blurb");
        assertThat(Given.inject(TagRepository.class).findBySlug("news")).isEmpty();
    }

    @Test
    void missing_tag_id_returns_bad_request_toast() {
        authorized(editor).contentType("application/x-www-form-urlencoded")
                          .formParam("name", "X")
                          .formParam("slug", "y")
                          .post("/forms/tags/update")
                          .then()
                          .statusCode(400)
                          .header("X-Toast-Type", equalTo("Error"))
                          .header("X-Toast-Message", equalTo("Missing tag."));
    }

    @Test
    void plain_user_returns_forbidden() {
        authorized(reader).contentType("application/x-www-form-urlencoded")
                          .formParam("tagId", newsTagId)
                          .formParam("name", "News")
                          .formParam("slug", "news")
                          .post("/forms/tags/update")
                          .then()
                          .statusCode(403);
    }

    @BeforeEach
    void setup() {
        Given.cleanup();

        RestAssuredPort.configure(baseUrl);

        editor = Given.user()
                      .withUsername("tageditor")
                      .withEmail("editor@example.com")
                      .withPassword("editorPw1")
                      .withName("Tag Editor")
                      .withRole(Role.EDITOR)
                      .persist();
        reader = Given.user()
                      .withUsername("tagreader")
                      .withEmail("reader@example.com")
                      .withPassword("readerPw1")
                      .withName("Tag Reader")
                      .persist();

        Given.post()
             .withTitle("Tagged Article")
             .withSlug("tagged-post")
             .withDescription("d")
             .withContent("body")
             .withAuthor(editor)
             .withTags("news", "other")
             .persist();

        newsTagId = Given.transaction(() -> Given.inject(TagRepository.class)
                                                 .findBySlug("news")
                                                 .map(Tag::getId)
                                                 .orElseThrow());
    }

    @Test
    void slug_that_slugifies_to_empty_returns_error_toast() {
        authorized(editor).contentType("application/x-www-form-urlencoded")
                          .formParam("tagId", newsTagId)
                          .formParam("name", "News")
                          .formParam("slug", "---")
                          .post("/forms/tags/update")
                          .then()
                          .statusCode(400)
                          .header("X-Toast-Message", equalTo(ERROR_SLUG));
    }

    @Test
    void slug_with_invalid_characters_returns_error_toast() {
        authorized(editor).contentType("application/x-www-form-urlencoded")
                          .formParam("tagId", newsTagId)
                          .formParam("name", "News")
                          .formParam("slug", "bad_slug")
                          .post("/forms/tags/update")
                          .then()
                          .statusCode(400)
                          .header("X-Toast-Message", equalTo(ERROR_SLUG));
    }

    @Test
    void unknown_tag_id_returns_not_found() {
        authorized(editor).contentType("application/x-www-form-urlencoded")
                          .formParam("tagId", 999_999_999L)
                          .formParam("name", "X")
                          .formParam("slug", "y")
                          .post("/forms/tags/update")
                          .then()
                          .statusCode(404);
    }
}
