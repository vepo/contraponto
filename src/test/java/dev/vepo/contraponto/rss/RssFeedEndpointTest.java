package dev.vepo.contraponto.rss;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResource;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;

@QuarkusIntegrationTest
class RssFeedEndpointTest {

    private static final class RestAssuredPort {
        static void configure(URL baseUrl) {
            io.restassured.RestAssured.baseURI = baseUrl.getProtocol() + "://" + baseUrl.getHost();
            io.restassured.RestAssured.port = baseUrl.getPort();
            io.restassured.RestAssured.basePath = "";
        }

        private RestAssuredPort() {}
    }

    @TestHTTPResource("/")
    URL baseUrl;

    private User alice;
    private Blog sideBlog;

    @Test
    void main_blog_feed_lists_only_main_blog() {
        String xml = given().get("/rssalice/feed/main-blog").then().statusCode(200).extract().body().asString();
        assertThat(xml).contains("Main Post One").doesNotContain("Side Post One");
    }

    @Test
    void secondary_blog_feed() {
        String xml = given().get("/rssalice/sideblog/feed").then().statusCode(200).extract().body().asString();
        assertThat(xml).contains("Side Post One").doesNotContain("Main Post One");
    }

    @Test
    void serie_feed_main_blog() {
        String xml = given().get("/rssalice/serie/my-series/feed").then().statusCode(200).extract().body().asString();
        assertThat(xml).contains("Series Post");
    }

    @BeforeEach
    void setup() {
        Given.cleanup();
        RestAssuredPort.configure(baseUrl);

        alice = Given.user()
                     .withUsername("rssalice")
                     .withEmail("rssalice@example.com")
                     .withPassword("pw123456789")
                     .withName("Alice RSS")
                     .persist();

        sideBlog = Given.blog()
                        .withUser(alice)
                        .withSlug("sideblog")
                        .withName("Side Blog")
                        .withDescription("Side desc")
                        .persist();

        Given.post()
             .withTitle("Main Post One")
             .withSlug("main-one")
             .withDescription("Main post summary")
             .withContent("# Hello")
             .withAuthor(alice)
             .withTags("rss-topic")
             .persist();

        Given.post()
             .withTitle("Side Post One")
             .withSlug("side-one")
             .withDescription("Side summary")
             .withContent("Side body")
             .withBlog(sideBlog)
             .withAuthor(alice)
             .withTags("rss-topic")
             .persist();

        Given.post()
             .withTitle("Series Post")
             .withSlug("series-a")
             .withSerieTitle("My Series")
             .withContent("Series body")
             .withAuthor(alice)
             .persist();
    }

    @Test
    void site_feed_returns_rss_with_items() {
        String xml = given().get("/feed").then().statusCode(200).extract().body().asString();
        assertThat(xml).contains("<rss version=\"2.0\">").contains("Main Post One").contains("Side Post One");
    }

    @Test
    void tag_feed() {
        String xml = given().get("/tags/rss-topic/feed").then().statusCode(200).extract().body().asString();
        assertThat(xml).contains("Main Post One").contains("Side Post One");
    }

    @Test
    void unknown_user_feed_404() {
        given().get("/nobody12345/feed").then().statusCode(404);
    }

    @Test
    void user_feed_lists_all_blogs() {
        String xml = given().get("/rssalice/feed").then().statusCode(200).extract().body().asString();
        assertThat(xml).contains("Main Post One").contains("Side Post One");
    }
}
