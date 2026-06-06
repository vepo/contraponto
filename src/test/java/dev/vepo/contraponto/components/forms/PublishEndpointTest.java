package dev.vepo.contraponto.components.forms;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostPublicationRepository;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.postresponse.PostResponseLinkBackStatus;
import dev.vepo.contraponto.postresponse.PostResponseRepository;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.TestHttp;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResource;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class PublishEndpointTest {

    @TestHTTPResource("/")
    URL baseUrl;

    @Inject
    PostRepository postRepository;

    @Inject
    PostPublicationRepository publicationRepository;

    @Inject
    PostResponseRepository postResponseRepository;

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

    @Test
    void publish_presavedDraftWithEmptySlugDerivesSlugFromTitle() {
        authorized().contentType("application/x-www-form-urlencoded")
                    .formParam("blogId", author.getDefaultBlog().getId())
                    .formParam("title", "Fresh Story")
                    .formParam("slug", "")
                    .formParam("content", "Draft body")
                    .post("/forms/write/draft")
                    .then()
                    .statusCode(200);

        var draft = postRepository.findDrafts()
                                  .stream()
                                  .filter(p -> author.getId().equals(p.getBlog().getOwner().getId()))
                                  .filter(p -> "Fresh Story".equals(p.getTitle()))
                                  .findFirst()
                                  .orElseThrow();
        assertThat(draft.getSlug()).isBlank();

        authorized().contentType("application/x-www-form-urlencoded")
                    .formParam("postId", draft.getId())
                    .formParam("blogId", author.getDefaultBlog().getId())
                    .formParam("title", "Fresh Story")
                    .formParam("slug", "")
                    .formParam("content", "Draft body")
                    .post("/forms/write/publish")
                    .then()
                    .statusCode(200)
                    .body(containsString("Draft body"));

        var published = Given.transaction(() -> {
            Given.inject(jakarta.persistence.EntityManager.class).clear();
            return postRepository.findByIdWithTags(draft.getId()).orElseThrow();
        });
        assertThat(published.getSlug()).isEqualTo("fresh-story");
        assertThat(published.isPublished()).isTrue();
    }

    @Test
    void publish_with_respondsTo_creates_pending_post_response() {
        User responder = Given.user()
                              .withUsername("responseauthor")
                              .withEmail("responseauthor@example.com")
                              .withPassword("pw123456789")
                              .withName("Response Author")
                              .persist();
        Post source = Given.post()
                           .withAuthor(author)
                           .withTitle("Source for response")
                           .withSlug("source-for-response")
                           .withContent("Source body")
                           .withPublished(true)
                           .persist();

        TestHttp.authenticated(responder)
                .contentType("application/x-www-form-urlencoded")
                .formParam("blogId", responder.getDefaultBlog().getId())
                .formParam("title", "My response essay")
                .formParam("slug", "my-response-essay")
                .formParam("description", "Response summary")
                .formParam("content", "Response essay body")
                .formParam("respondsTo", source.getId())
                .post("/forms/write/publish")
                .then()
                .statusCode(200)
                .body(containsString("Response essay body"));

        Post responsePost = postRepository.findMainBlogPost(responder.getUsername(), "my-response-essay").orElseThrow();
        assertThat(responsePost.isPublished()).isTrue();

        var response = postResponseRepository.findByResponsePostId(responsePost.getId()).orElseThrow();
        assertThat(response.getSourcePost().getId()).isEqualTo(source.getId());
        assertThat(response.getResponder().getId()).isEqualTo(responder.getId());
        assertThat(response.getLinkBackStatus()).isEqualTo(PostResponseLinkBackStatus.PENDING);
    }

    @Test
    void publish_withoutSavingDraftDerivesSlugFromTitle() {
        authorized().contentType("application/x-www-form-urlencoded")
                    .formParam("blogId", author.getDefaultBlog().getId())
                    .formParam("title", "Direct Publish")
                    .formParam("slug", "")
                    .formParam("content", "Published without save")
                    .post("/forms/write/publish")
                    .then()
                    .statusCode(200)
                    .body(containsString("Published without save"));

        var published = Given.transaction(() -> {
            Given.inject(jakarta.persistence.EntityManager.class).clear();
            return postRepository.findMainBlogPost(author.getUsername(), "direct-publish").orElseThrow();
        });
        assertThat(published.getSlug()).isEqualTo("direct-publish");
        assertThat(published.isPublished()).isTrue();
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
