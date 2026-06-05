package dev.vepo.contraponto.seo;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.infra.TemplateExtensions;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResource;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;

@QuarkusIntegrationTest
class SeoEndpointTest {

    @TestHTTPResource
    URL baseUrl;

    @Test
    void feedHasNoindexHeader() {
        given().when()
               .get("/feed")
               .then()
               .statusCode(200)
               .header("X-Robots-Tag", "noindex");
    }

    @Test
    void postPageHasCanonicalAndStructuredData() {
        User author = Given.user()
                           .withUsername("seo-meta")
                           .withEmail("seo-meta@example.com")
                           .withName("SEO Meta Author")
                           .withPassword("seopass123")
                           .persist();
        var post = Given.post()
                        .withTitle("Canonical Post")
                        .withSlug("canonical-post")
                        .withDescription("Short description for meta")
                        .withContent("Body content for the canonical post.")
                        .withAuthor(author)
                        .persist();
        String postPath = TemplateExtensions.url(post);

        String html = given().when()
                             .get(postPath)
                             .then()
                             .statusCode(200)
                             .body(containsString("rel=\"canonical\""))
                             .body(containsString("property=\"og:title\""))
                             .body(containsString("BlogPosting"))
                             .extract()
                             .asString();
        int canonicalCount = html.split("rel=\"canonical\"", -1).length - 1;
        org.assertj.core.api.Assertions.assertThat(canonicalCount).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(html).contains("<html lang=\"pt-BR\">");
    }

    @Test
    void republishedPostHasModifiedTimeMeta() {
        User author = Given.user()
                           .withUsername("seo-modified")
                           .withEmail("seo-modified@example.com")
                           .withName("SEO Modified Author")
                           .withPassword("seopass123")
                           .persist();
        var post = Given.post()
                        .withTitle("Modified Post")
                        .withSlug("modified-post")
                        .withDescription("First version")
                        .withContent("Version one body.")
                        .withAuthor(author)
                        .persist();
        Given.transaction(() -> {
            var reloaded = Given.inject(dev.vepo.contraponto.post.PostRepository.class)
                                .findByIdWithTags(post.getId())
                                .orElseThrow();
            reloaded.setContent("Version two body.");
            Given.inject(dev.vepo.contraponto.post.PostPublicationService.class).publish(reloaded);
        });
        String postPath = "/" + author.getUsername() + "/post/" + post.getSlug();

        given().when()
               .get(postPath)
               .then()
               .statusCode(200)
               .body(containsString("article:modified_time"));
    }

    @Test
    void robotsTxtReferencesSitemap() {
        given().when()
               .get("/robots.txt")
               .then()
               .statusCode(200)
               .body(containsString("Sitemap:"))
               .body(containsString("/sitemap.xml"));
    }

    @BeforeEach
    void setup() {
        Given.cleanup();
        io.restassured.RestAssured.baseURI = baseUrl.getProtocol() + "://" + baseUrl.getHost();
        io.restassured.RestAssured.port = baseUrl.getPort();
        io.restassured.RestAssured.basePath = "";
    }

    @Test
    void sitemapContainsPublishedPost() {
        User author = Given.user()
                           .withUsername("seo-author")
                           .withEmail("seo@example.com")
                           .withName("SEO Author")
                           .withPassword("seopass123")
                           .persist();
        var post = Given.post()
                        .withTitle("SEO Test Post")
                        .withSlug("seo-test-post")
                        .withDescription("A post for sitemap coverage")
                        .withContent("Published body for SEO test.")
                        .withAuthor(author)
                        .persist();
        String postPath = "/" + author.getUsername() + "/post/" + post.getSlug();

        given().when()
               .get("/sitemap.xml")
               .then()
               .statusCode(200)
               .body(containsString(postPath));
    }
}
