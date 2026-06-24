package dev.vepo.contraponto.post;

import static org.hamcrest.Matchers.equalTo;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.shared.TestHttp;
import dev.vepo.contraponto.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusIntegrationTest
class PostManagementEndpointTest {

    private User alice;
    private User bob;
    private Blog aliceBlog;
    private Post draft;
    private Post published;

    @Test
    void deleteDraftRequiresCsrf() {
        TestHttp.session(alice).when()
                .delete("/forms/posts/" + draft.getId())
                .then()
                .statusCode(403);
    }

    @Test
    void deleteDraftSucceedsWithCsrf() {
        TestHttp.authenticated(alice).when()
                .delete("/forms/posts/" + draft.getId())
                .then()
                .statusCode(200)
                .header("X-Toast-I18n-Key", equalTo("toast.post.deleted"));
    }

    @Test
    void deletePublishedReturnsBadRequest() {
        TestHttp.authenticated(alice).when()
                .delete("/forms/posts/" + published.getId())
                .then()
                .statusCode(400)
                .header("X-Toast-I18n-Key", equalTo("toast.post.deletePublished"));
    }

    @Test
    void foreignUserGetsNotFoundOnDelete() {
        TestHttp.authenticated(bob).when()
                .delete("/forms/posts/" + draft.getId())
                .then()
                .statusCode(404)
                .header("X-Toast-I18n-Key", equalTo("toast.post.notFound"));
    }

    @Test
    void foreignUserGetsNotFoundOnUnpublish() {
        TestHttp.authenticated(bob).when()
                .post("/forms/posts/" + published.getId() + "/unpublish")
                .then()
                .statusCode(404)
                .header("X-Toast-I18n-Key", equalTo("toast.post.notFound"));
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        alice = Given.user()
                     .withUsername("alice-endpoint")
                     .withEmail("alice-endpoint@test.com")
                     .withName("Alice")
                     .withPassword("Password123!")
                     .persist();
        bob = Given.user()
                   .withUsername("bob-endpoint")
                   .withEmail("bob-endpoint@test.com")
                   .withName("Bob")
                   .withPassword("Password123!")
                   .persist();
        aliceBlog = alice.getDefaultBlog();
        draft = Given.post()
                     .withAuthor(alice)
                     .withBlog(aliceBlog)
                     .withTitle("Draft")
                     .withSlug("endpoint-draft")
                     .withContent("body")
                     .withPublished(false)
                     .persist();
        published = Given.post()
                         .withAuthor(alice)
                         .withBlog(aliceBlog)
                         .withTitle("Published")
                         .withSlug("endpoint-published")
                         .withContent("body")
                         .withPublished(true)
                         .persist();
    }

    @Test
    void unpublishRequiresCsrf() {
        TestHttp.session(alice).when()
                .post("/forms/posts/" + published.getId() + "/unpublish")
                .then()
                .statusCode(403);
    }

    @Test
    void unpublishSucceedsWithCsrf() {
        TestHttp.authenticated(alice).when()
                .post("/forms/posts/" + published.getId() + "/unpublish")
                .then()
                .statusCode(200)
                .header("X-Toast-I18n-Key", equalTo("toast.post.unpublished"));
    }
}
