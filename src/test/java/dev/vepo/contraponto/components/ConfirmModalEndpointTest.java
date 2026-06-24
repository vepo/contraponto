package dev.vepo.contraponto.components;

import static org.hamcrest.Matchers.containsString;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.shared.TestHttp;
import dev.vepo.contraponto.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusIntegrationTest
class ConfirmModalEndpointTest {

    private User alice;
    private User bob;
    private Blog aliceBlog;
    private Post draft;
    private Post published;

    @Test
    void foreignUserGetsNotFoundForModal() {
        TestHttp.authenticated(bob).when()
                .get("/components/confirm-modal/post-delete/" + draft.getId())
                .then()
                .statusCode(404);
    }

    @Test
    void postDeleteModalIncludesActionForOwner() {
        TestHttp.authenticated(alice).when()
                .get("/components/confirm-modal/post-delete/" + draft.getId())
                .then()
                .statusCode(200)
                .body(containsString("hx-delete=\"/forms/posts/" + draft.getId() + "\""));
    }

    @Test
    void postUnpublishModalIncludesActionForOwner() {
        TestHttp.authenticated(alice).when()
                .get("/components/confirm-modal/post-unpublish/" + published.getId())
                .then()
                .statusCode(200)
                .body(containsString("id=\"confirmModal\""))
                .body(containsString("hx-post=\"/forms/posts/" + published.getId() + "/unpublish\""))
                .body(containsString("data-confirm-submit"));
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        alice = Given.user()
                     .withUsername("alice-confirm")
                     .withEmail("alice-confirm@test.com")
                     .withName("Alice")
                     .withPassword("Password123!")
                     .persist();
        bob = Given.user()
                   .withUsername("bob-confirm")
                   .withEmail("bob-confirm@test.com")
                   .withName("Bob")
                   .withPassword("Password123!")
                   .persist();
        aliceBlog = alice.getDefaultBlog();
        draft = Given.post()
                     .withAuthor(alice)
                     .withBlog(aliceBlog)
                     .withTitle("Draft")
                     .withSlug("confirm-draft")
                     .withContent("body")
                     .withPublished(false)
                     .persist();
        published = Given.post()
                         .withAuthor(alice)
                         .withBlog(aliceBlog)
                         .withTitle("Published")
                         .withSlug("confirm-published")
                         .withContent("body")
                         .withPublished(true)
                         .persist();
    }
}
