package dev.vepo.contraponto.post;

import static io.restassured.RestAssured.given;

import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResource;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@QuarkusIntegrationTest
class PostEndpointReferrerTest {

    @TestHTTPResource
    URL baseUrl;

    @Inject
    EntityManager entityManager;

    private User author;

    @Test
    void recordsNormalizedReferrerHostFromRefererHeader() {
        var post = Given.post()
                        .withAuthor(author)
                        .withTitle("Referrer test")
                        .withSlug("referrer-post")
                        .withContent("Body")
                        .withPublished(true)
                        .persist();

        given().header("Referer", "https://www.google.com/search?q=contraponto")
               .when()
               .get(PostPaths.extractUrl(post))
               .then()
               .statusCode(200);

        assertThat(referrerHostFor(post)).isEqualTo("www.google.com");
    }

    @Test
    void recordsNullReferrerHostWhenRefererHeaderAbsent() {
        var post = Given.post()
                        .withAuthor(author)
                        .withTitle("No referrer test")
                        .withSlug("no-referrer-post")
                        .withContent("Body")
                        .withPublished(true)
                        .persist();

        given().when()
               .get(PostPaths.extractUrl(post))
               .then()
               .statusCode(200);

        assertThat(referrerHostFor(post)).isNull();
    }

    @Test
    void recordsNullReferrerHostWhenRefererHeaderMalformed() {
        var post = Given.post()
                        .withAuthor(author)
                        .withTitle("Malformed referrer test")
                        .withSlug("malformed-referrer-post")
                        .withContent("Body")
                        .withPublished(true)
                        .persist();

        given().header("Referer", "not a valid uri")
               .when()
               .get(PostPaths.extractUrl(post))
               .then()
               .statusCode(200);

        assertThat(referrerHostFor(post)).isNull();
    }

    private String referrerHostFor(Post post) {
        return (String) entityManager.createNativeQuery("""
                                                        SELECT referrer_host FROM tb_views
                                                        WHERE post_id = :postId
                                                        ORDER BY id DESC
                                                        LIMIT 1
                                                        """)
                                     .setParameter("postId", post.getId())
                                     .getSingleResult();
    }

    @BeforeEach
    void setup() {
        Given.cleanup();
        io.restassured.RestAssured.baseURI = baseUrl.getProtocol() + "://" + baseUrl.getHost();
        io.restassured.RestAssured.port = baseUrl.getPort();
        io.restassured.RestAssured.basePath = "";
        author = Given.user()
                      .withUsername("referrer-author")
                      .withEmail("referrer-author@test.com")
                      .withName("Referrer Author")
                      .withPassword("Password123!")
                      .persist();
    }
}
