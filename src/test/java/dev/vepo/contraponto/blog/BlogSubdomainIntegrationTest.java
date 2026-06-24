package dev.vepo.contraponto.blog;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
@TestProfile(BlogSubdomainTestProfile.class)
class BlogSubdomainIntegrationTest {

    @TestHTTPResource("/")
    URL baseUrl;

    @Inject
    BlogPublicUrlService blogPublicUrlService;

    private User author;
    private Post post;

    @Test
    void canonicalUrl_usesSubdomainWhenEnabled() {
        assertThat(blogPublicUrlService.canonicalOrPlatformAbsolute(post)).isEqualTo("https://subdom-author.localhost/post/subdomain-post");
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        author = Given.user()
                      .withUsername("subdom-author")
                      .withEmail("subdom-author@test.com")
                      .withName("Subdomain Author")
                      .withPassword("password123")
                      .persist();
        post = Given.post()
                    .withAuthor(author)
                    .withTitle("Subdomain Post")
                    .withSlug("subdomain-post")
                    .withContent("Body")
                    .withPublished(true)
                    .persist();
        io.restassured.RestAssured.baseURI = "http://127.0.0.1";
        io.restassured.RestAssured.port = baseUrl.getPort();
        io.restassured.RestAssured.basePath = "";
    }

    @Test
    void subdomainHost_platformRouteRedirectsToPlatformHost() {
        given().header("Host", "subdom-author.localhost")
               .redirects()
               .follow(false)
               .when()
               .get("/manage")
               .then()
               .statusCode(302)
               .header("Location", "https://blogs.localhost/manage");
    }

    @Test
    void subdomainHost_servesMainBlogPost() {
        given().header("Host", "subdom-author.localhost")
               .redirects()
               .follow(false)
               .when()
               .get("/post/subdomain-post")
               .then()
               .statusCode(200)
               .body(containsString("Subdomain Post"));
    }
}
