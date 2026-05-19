package dev.vepo.contraponto.rss;

import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.post.Post;
import java.net.URL;

import dev.vepo.contraponto.shared.Given;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
class RssFeedLinkWebTest {

    @TestHTTPResource("/")
    URL baseUrl;

    private Blog blog;
    private String tagSlug;

    @Test
    void blogPageShowsBlogRssLink() {
        RestAssured.get("/" + blog.getOwner().getUsername())
                   .then()
                   .statusCode(200)
                   .body(containsString("/feed/main-blog"))
                   .body(containsString("rss-feed-link"))
                   .body(containsString("RSS"));
    }

    @Test
    void homePageShowsSiteRssLink() {
        RestAssured.get("/")
                   .then()
                   .statusCode(200)
                   .body(containsString("href=\"/feed\""))
                   .body(containsString("rss-feed-link"))
                   .body(containsString("RSS"));
    }

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = baseUrl.getProtocol() + "://" + baseUrl.getHost();
        RestAssured.port = baseUrl.getPort();
        RestAssured.basePath = "";
        Given.cleanup();
        var author = Given.user()
                          .withUsername("rssauthor")
                          .withEmail("rssauthor@test.com")
                          .withName("RSS Author")
                          .withPassword("password123")
                          .persist();
        blog = author.getDefaultBlog();
        tagSlug = "rss-topic";
        Given.post()
             .withAuthor(author)
             .withBlog(blog)
             .withTitle("RSS Post")
             .withSlug("rss-post")
             .withContent("Enough words for a published post about RSS feeds and syndication.")
             .withPublished(true)
             .withTags("rss-topic")
             .persist();
    }

    @Test
    void tagPageShowsTagRssLink() {
        RestAssured.get("/tags/" + tagSlug)
                   .then()
                   .statusCode(200)
                   .body(containsString("/tags/" + tagSlug + "/feed"))
                   .body(containsString("rss-feed-link"))
                   .body(containsString("RSS"));
    }
}
