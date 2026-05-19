package dev.vepo.contraponto.readingtime;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.TestHttp;
import dev.vepo.contraponto.shared.security.CsrfTokenService;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.view.SessionIdProvider;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@QuarkusTest
class ReadingTimeEndpointTest {

    @TestHTTPResource("/")
    URL baseUrl;

    @Inject
    EntityManager entityManager;

    private User author;

    @Test
    void draftPostReturnsNotFound() {
        var post = Given.post()
                        .withAuthor(author)
                        .withTitle("Draft")
                        .withContent("Body")
                        .withSlug("draft-post")
                        .withPublished(false)
                        .persist();

        TestHttp.authenticated(author)
                .when()
                .post("/forms/posts/" + post.getId() + "/reading-time")
                .then()
                .statusCode(404);
    }

    @Test
    void heartbeatAccumulatesSecondsForPublishedPost() {
        var post = Given.post()
                        .withAuthor(author)
                        .withTitle("Readable")
                        .withContent("Body")
                        .withSlug("readable-post")
                        .withPublished(true)
                        .persist();

        var bootstrap = given().redirects().follow(true).when().get("/");
        String csrf = bootstrap.getCookie(CsrfTokenService.COOKIE_NAME);

        given().cookie(CsrfTokenService.COOKIE_NAME, csrf)
               .cookie(SessionIdProvider.VIEW_SESSION_COOKIE, "anon-read-test")
               .header(CsrfTokenService.HEADER_NAME, csrf)
               .when()
               .post("/forms/posts/" + post.getId() + "/reading-time")
               .then()
               .statusCode(204);

        given().cookie(CsrfTokenService.COOKIE_NAME, csrf)
               .cookie(SessionIdProvider.VIEW_SESSION_COOKIE, "anon-read-test")
               .header(CsrfTokenService.HEADER_NAME, csrf)
               .when()
               .post("/forms/posts/" + post.getId() + "/reading-time")
               .then()
               .statusCode(204);

        Number total = (Number) entityManager.createNativeQuery("""
                                                                SELECT total_seconds FROM tb_reading_sessions
                                                                WHERE post_id = :postId AND session_id = :sessionId
                                                                """)
                                             .setParameter("postId", post.getId())
                                             .setParameter("sessionId", "anon-read-test")
                                             .getSingleResult();
        assertThat(total.intValue()).isEqualTo(10);
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        author = Given.user()
                      .withUsername("rtauthor")
                      .withEmail("rtauthor@test.com")
                      .withName("RT Author")
                      .withPassword("Password123!")
                      .persist();
    }
}
