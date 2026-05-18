package dev.vepo.contraponto.components.forms;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.post.PostPublicationRepository;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.TestHttp;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class PublishEndpointTest {

    @TestHTTPResource("/")
    URL baseUrl;

    @Inject
    PostRepository postRepository;

    @Inject
    PostPublicationRepository publicationRepository;

    private User author;

    private io.restassured.specification.RequestSpecification authorized() {
        return TestHttp.authenticated(author);
    }

    @Test
    void draft_save_does_not_change_public_content_until_republish() {
        var post = Given.post()
                        .withAuthor(author)
                        .withTitle("Public")
                        .withContent("Live body")
                        .withSlug("draft-separation")
                        .withPublished(true)
                        .persist();

        authorized().contentType("application/x-www-form-urlencoded")
                    .formParam("postId", post.getId())
                    .formParam("blogId", post.getBlog().getId())
                    .formParam("title", "Public")
                    .formParam("slug", "draft-separation")
                    .formParam("description", "Summary")
                    .formParam("content", "Unpublished draft body")
                    .post("/forms/write/draft")
                    .then()
                    .statusCode(200);

        var reloaded = postRepository.findMainBlogPost(author.getUsername(), "draft-separation").orElseThrow();
        assertThat(reloaded.getContent()).isEqualTo("Unpublished draft body");
        assertThat(reloaded.getLivePublication().getContent()).isEqualTo("Live body");

        authorized().contentType("application/x-www-form-urlencoded")
                    .formParam("postId", post.getId())
                    .formParam("blogId", post.getBlog().getId())
                    .formParam("title", "Public")
                    .formParam("slug", "draft-separation")
                    .formParam("description", "Summary")
                    .formParam("content", "Live body v2")
                    .post("/forms/write/publish")
                    .then()
                    .statusCode(200)
                    .body(containsString("Live body v2"));

        var versions = publicationRepository.findByPostIdOrderByVersionDesc(post.getId());
        assertThat(versions).hasSize(2);
        assertThat(versions.getFirst().getContent()).isEqualTo("Live body v2");
        assertThat(versions.getFirst().getVersion()).isEqualTo(2);

        reloaded = postRepository.findByIdWithTags(post.getId()).orElseThrow();
        assertThat(reloaded.getLivePublication().getId()).isEqualTo(versions.getFirst().getId());
        assertThat(reloaded.getLivePublication().getContent()).isEqualTo("Live body v2");
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        io.restassured.RestAssured.baseURI = baseUrl.getProtocol() + "://" + baseUrl.getHost();
        io.restassured.RestAssured.port = baseUrl.getPort();
        io.restassured.RestAssured.basePath = "";
        author = Given.user()
                      .withUsername("publishauthor")
                      .withEmail("publishauthor@example.com")
                      .withPassword("pw123456789")
                      .withName("Publish Author")
                      .persist();
    }
}
