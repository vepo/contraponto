package dev.vepo.contraponto.post;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.shared.infra.TemplateExtensions;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResource;

@QuarkusIntegrationTest
class PostRelatedPostsTest {

    @TestHTTPResource
    URL baseUrl;

    @Test
    void relatedPostsAppearInAsideAfterArticleContent() {
        User author = Given.user()
                           .withUsername("related-author")
                           .withEmail("related-author@example.com")
                           .withName("Related Author")
                           .withPassword("relatedpass123")
                           .persist();
        var relatedPost = Given.post()
                               .withTitle("Companion Article")
                               .withSlug("companion-article")
                               .withContent("Companion body.")
                               .withAuthor(author)
                               .withTags("shared-topic")
                               .persist();
        var primaryPost = Given.post()
                               .withTitle("Primary Article")
                               .withSlug("primary-article")
                               .withContent("Primary body.")
                               .withAuthor(author)
                               .withTags("shared-topic")
                               .persist();
        String postPath = TemplateExtensions.url(primaryPost);

        String html = given().when()
                             .get(postPath)
                             .then()
                             .statusCode(200)
                             .body(containsString("post-related-aside"))
                             .body(containsString("Companion Article"))
                             .extract()
                             .asString();

        int contentIndex = html.indexOf("article-page__content");
        int asideIndex = html.indexOf("post-related-aside");
        assertThat(contentIndex).isGreaterThan(-1);
        assertThat(asideIndex).isGreaterThan(contentIndex);
        assertThat(html).contains(TemplateExtensions.url(relatedPost));
    }

    @BeforeEach
    void setup() {
        Given.cleanup();
        io.restassured.RestAssured.baseURI = baseUrl.getProtocol() + "://" + baseUrl.getHost();
        io.restassured.RestAssured.port = baseUrl.getPort();
        io.restassured.RestAssured.basePath = "";
    }
}
