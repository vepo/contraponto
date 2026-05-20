package dev.vepo.contraponto.components.forms;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.image.Image;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.TestHttp;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResource;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class SaveDraftEndpointTest {

    @TestHTTPResource("/")
    URL baseUrl;

    @Inject
    PostRepository postRepository;

    private User author;

    @Test
    void preservesCoverWhenCoverIdOmitted() {
        Image cover = Given.randomCover(author.getDefaultBlog());
        var post = Given.post()
                        .withAuthor(author)
                        .withCover(cover)
                        .withTitle("With cover")
                        .withContent("Body")
                        .withSlug("with-cover")
                        .withPublished(false)
                        .persist();

        TestHttp.authenticated(author)
                .contentType("application/x-www-form-urlencoded")
                .formParam("postId", post.getId())
                .formParam("blogId", post.getBlog().getId())
                .formParam("title", "With cover")
                .formParam("slug", "with-cover")
                .formParam("description", "Summary")
                .formParam("content", "Updated body")
                .post("/forms/write/draft")
                .then()
                .statusCode(200);

        var reloaded = postRepository.findByIdWithTags(post.getId()).orElseThrow();
        assertThat(reloaded.getContent()).isEqualTo("Updated body");
        assertThat(reloaded.getCover()).isNotNull();
        assertThat(reloaded.getCover().getUuid()).isEqualTo(cover.getUuid());
    }

    @Test
    void rejectsBlankContent() {
        TestHttp.authenticated(author)
                .contentType("application/x-www-form-urlencoded")
                .formParam("blogId", author.getDefaultBlog().getId())
                .formParam("title", "Title")
                .formParam("slug", "my-slug")
                .formParam("content", " ")
                .post("/forms/write/draft")
                .then()
                .statusCode(200)
                .header("X-Toast-Message", containsString("Content is required"));
    }

    @Test
    void rejectsBlankTitle() {
        TestHttp.authenticated(author)
                .contentType("application/x-www-form-urlencoded")
                .formParam("blogId", author.getDefaultBlog().getId())
                .formParam("title", " ")
                .formParam("slug", "my-slug")
                .formParam("content", "Body")
                .post("/forms/write/draft")
                .then()
                .statusCode(200)
                .header("X-Toast-Message", containsString("Title is required"));
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        io.restassured.RestAssured.baseURI = baseUrl.getProtocol() + "://" + baseUrl.getHost();
        io.restassured.RestAssured.port = baseUrl.getPort();
        io.restassured.RestAssured.basePath = "";
        author = Given.user()
                      .withUsername("draftauth")
                      .withEmail("draftauth@example.com")
                      .withPassword("Password123!")
                      .withName("Draft Author")
                      .persist();
    }
}
