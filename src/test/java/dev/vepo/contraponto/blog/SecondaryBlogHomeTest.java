package dev.vepo.contraponto.blog;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import java.net.URL;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResource;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;

@QuarkusIntegrationTest
class SecondaryBlogHomeTest {

    @TestHTTPResource("/")
    URL baseUrl;

    private User author;
    private Blog secondaryBlog;

    @Test
    void secondaryBlogGridLoadMoreUsesBlogScopedPath() {
        given().get("/" + author.getUsername() + "/" + secondaryBlog.getSlug())
               .then()
               .statusCode(200)
               .body(containsString("/" + author.getUsername() + "/" + secondaryBlog.getSlug() + "/components/grid?page=2"));
    }

    @Test
    void secondaryBlogHomeIsReachable() {
        given().get("/" + author.getUsername() + "/" + secondaryBlog.getSlug())
               .then()
               .statusCode(200)
               .body(containsString("Architecture Notes"))
               .body(containsString("<strong>Secondary</strong>"))
               .body(containsString("Secondary Post 1"));
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        io.restassured.RestAssured.baseURI = baseUrl.getProtocol() + "://" + baseUrl.getHost();
        io.restassured.RestAssured.port = baseUrl.getPort();
        io.restassured.RestAssured.basePath = "";

        author = Given.user()
                      .withUsername("multiblog")
                      .withEmail("multi@test.com")
                      .withName("Multi Blog Author")
                      .withPassword("pass")
                      .persist();

        secondaryBlog = Given.blog()
                             .withUser(author)
                             .withName("Architecture Notes")
                             .withSlug("architecture-notes")
                             .withDescription("**Secondary** blog description")
                             .persist();

        String content = "Body with enough words for read time on the secondary blog.";
        IntStream.range(1, 16)
                 .forEach(i -> Given.post()
                                    .withAuthor(author)
                                    .withBlog(secondaryBlog)
                                    .withTitle("Secondary Post " + i)
                                    .withSlug("secondary-post-" + i)
                                    .withContent(content)
                                    .withPublished(true)
                                    .persist());
    }
}
