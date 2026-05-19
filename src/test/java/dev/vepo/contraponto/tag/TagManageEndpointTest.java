package dev.vepo.contraponto.tag;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.components.forms.LoginEndpoint;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.infra.LoggedUserProvider;
import dev.vepo.contraponto.user.Role;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class TagManageEndpointTest {

    @TestHTTPResource("/")
    URL baseUrl;

    private User editor;
    private User reader;

    @Test
    void editor_can_open_manage_and_edit_pages() {
        var sessionId = Given.inject(LoggedUserProvider.class).login(editor).getSessionId();

        given().cookie(LoginEndpoint.SESSION_COOKIE_NAME, sessionId)
               .get("/editor/tags")
               .then()
               .statusCode(200)
               .body(containsString("Tags"))
               .body(containsString("news"));

        given().cookie(LoginEndpoint.SESSION_COOKIE_NAME, sessionId)
               .get("/tags/news/edit")
               .then()
               .statusCode(200)
               .body(containsString("id=\"tagEditForm\""));
    }

    @Test
    void reader_cannot_open_manage_page() {
        var sessionId = Given.inject(LoggedUserProvider.class).login(reader).getSessionId();

        given().cookie(LoginEndpoint.SESSION_COOKIE_NAME, sessionId)
               .get("/editor/tags")
               .then()
               .statusCode(403);
    }

    @BeforeEach
    void setup() {
        Given.cleanup();
        io.restassured.RestAssured.baseURI = baseUrl.getProtocol() + "://" + baseUrl.getHost();
        io.restassured.RestAssured.port = baseUrl.getPort();
        io.restassured.RestAssured.basePath = "";

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
             .withTags("news")
             .persist();
    }
}
