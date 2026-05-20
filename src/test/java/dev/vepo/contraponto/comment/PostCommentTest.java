package dev.vepo.contraponto.comment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.TestHttp;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class PostCommentTest {

    private User author;
    private User reader;
    private User otherReader;
    private Post post;

    @Test
    void author_comment_is_auto_approved() {
        TestHttp.authenticated(author)
                .contentType("application/x-www-form-urlencoded")
                .formParam("body", "Thanks for reading!")
                .post("/forms/posts/" + post.getId() + "/comments")
                .then()
                .statusCode(200);

        given().get("/" + author.getUsername() + "/post/" + post.getSlug() + "/components/comments")
               .then()
               .statusCode(200)
               .body(containsString("Thanks for reading!"));
    }

    @Test
    void non_owner_cannot_approve() {
        TestHttp.authenticated(reader)
                .contentType("application/x-www-form-urlencoded")
                .formParam("body", "Needs approval")
                .post("/forms/posts/" + post.getId() + "/comments")
                .then()
                .statusCode(200);

        long commentId = Given.inject(PostCommentRepository.class)
                              .findRootComments(post.getId()).getFirst().getId();

        TestHttp.authenticated(otherReader)
                .post("/forms/posts/" + post.getId() + "/comments/" + commentId + "/approve")
                .then()
                .statusCode(403);
    }

    @Test
    void post_page_includes_comments_section() {
        given().get("/" + author.getUsername() + "/post/" + post.getSlug())
               .then()
               .statusCode(200)
               .body(containsString("id=\"comments\""))
               .body(containsString("/components/comments"))
               .body(containsString("loggedIn from:body"));
    }

    @Test
    void reader_comment_pending_until_owner_approves() {
        TestHttp.authenticated(reader)
                .contentType("application/x-www-form-urlencoded")
                .formParam("body", "Great article!")
                .post("/forms/posts/" + post.getId() + "/comments")
                .then()
                .statusCode(200);

        given().get("/" + author.getUsername() + "/post/" + post.getSlug() + "/components/comments")
               .then()
               .statusCode(200)
               .body(not(containsString("Great article!")));

        TestHttp.session(author)
                .get("/" + author.getUsername() + "/post/" + post.getSlug() + "/components/comments")
                .then()
                .statusCode(200)
                .body(containsString("Great article!"))
                .body(containsString("Moderação pendente"));

        long commentId = Given.inject(PostCommentRepository.class)
                              .findRootComments(post.getId()).getFirst().getId();

        TestHttp.authenticated(author)
                .post("/forms/posts/" + post.getId() + "/comments/" + commentId + "/approve")
                .then()
                .statusCode(200);

        given().get("/" + author.getUsername() + "/post/" + post.getSlug() + "/components/comments")
               .then()
               .statusCode(200)
               .body(containsString("Great article!"));
    }

    @Test
    void replies_visible_after_approval_and_reply_count_shown() {
        TestHttp.authenticated(author)
                .contentType("application/x-www-form-urlencoded")
                .formParam("body", "Root comment")
                .post("/forms/posts/" + post.getId() + "/comments")
                .then()
                .statusCode(200);

        long rootId = Given.inject(PostCommentRepository.class)
                           .findRootComments(post.getId()).getFirst().getId();

        TestHttp.authenticated(reader)
                .contentType("application/x-www-form-urlencoded")
                .formParam("body", "A reply")
                .post("/forms/posts/" + post.getId() + "/comments/" + rootId + "/replies")
                .then()
                .statusCode(200);

        given().get("/" + author.getUsername() + "/post/" + post.getSlug() + "/components/comments")
               .then()
               .statusCode(200)
               .body(not(containsString("1 reply")));

        long replyId = Given.inject(PostCommentRepository.class)
                            .findRepliesByRootId(rootId).getFirst().getId();

        TestHttp.authenticated(author)
                .post("/forms/posts/" + post.getId() + "/comments/" + replyId + "/approve")
                .then()
                .statusCode(200);

        given().get("/" + author.getUsername() + "/post/" + post.getSlug() + "/components/comments")
               .then()
               .statusCode(200)
               .body(containsString("1 reply"));

        given().get("/" + author.getUsername() + "/post/" + post.getSlug() + "/components/comments/" + rootId + "/replies")
               .then()
               .statusCode(200)
               .body(containsString("A reply"));
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        author = Given.user()
                      .withUsername("commentauthor")
                      .withEmail("commentauthor@test.com")
                      .withName("Comment Author")
                      .withPassword("password123")
                      .persist();

        reader = Given.user()
                      .withUsername("commentreader")
                      .withEmail("commentreader@test.com")
                      .withName("Comment Reader")
                      .withPassword("password123")
                      .persist();

        otherReader = Given.user()
                           .withUsername("otherreader")
                           .withEmail("otherreader@test.com")
                           .withName("Other Reader")
                           .withPassword("password123")
                           .persist();

        post = Given.post()
                    .withAuthor(author)
                    .withTitle("Commentable Post")
                    .withSlug("commentable-post")
                    .withContent("Post body")
                    .withPublished(true)
                    .persist();
    }
}
