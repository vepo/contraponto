package dev.vepo.contraponto.blog;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import java.net.URL;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResource;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;

@QuarkusIntegrationTest
class BlogGridEndpointTest {

    @TestHTTPResource("/")
    URL baseUrl;

    private User author;

    @Test
    void blogPageLoadMoreButtonUsesCorrectPath() {
        given().get("/" + author.getUsername())
               .then()
               .statusCode(200)
               .body(containsString("/" + author.getUsername() + "/components/grid?page=2"))
               .body(not(containsString("/components/home/grid")));
    }

    @Test
    void gridEndpointReturnsArticleCards() {
        given().get("/" + author.getUsername() + "/components/grid?page=2")
               .then()
               .statusCode(200)
               .body(containsString("article-card"))
               .body(containsString("Post 1"))
               .body(containsString("Post 2"));
    }

    @Test
    void legacyHomeGridPathIsNotFound() {
        given().get("/" + author.getUsername() + "/components/home/grid?page=2")
               .then()
               .statusCode(404);
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        io.restassured.RestAssured.baseURI = baseUrl.getProtocol() + "://" + baseUrl.getHost();
        io.restassured.RestAssured.port = baseUrl.getPort();
        io.restassured.RestAssured.basePath = "";

        author = Given.user()
                      .withUsername("gridauthor")
                      .withEmail("grid@test.com")
                      .withName("Grid Author")
                      .withPassword("pass")
                      .persist();

        String content = "Content of the blog post. Lorem ipsum dolor sit amet.";
        IntStream.range(1, 16)
                 .forEach(i -> Given.post()
                                    .withTitle("Post " + i)
                                    .withSlug("post-" + i)
                                    .withDescription("Description " + i)
                                    .withContent(content)
                                    .withAuthor(author)
                                    .persist());
    }
}
