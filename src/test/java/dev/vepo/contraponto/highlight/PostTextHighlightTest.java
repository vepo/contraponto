package dev.vepo.contraponto.highlight;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.postresponse.PostResponse;
import dev.vepo.contraponto.postresponse.PostResponseLinkBackStatus;
import dev.vepo.contraponto.postresponse.PostResponseRepository;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.TestHttp;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class PostTextHighlightTest {

    @Inject
    PostTextHighlightRepository highlightRepository;

    @Inject
    OfficialHighlightRepository officialHighlightRepository;

    @Inject
    CommonHighlightProposalRepository proposalRepository;

    @Inject
    PostResponseRepository postResponseRepository;

    private User author;
    private User reader;
    private User reader2;
    private User reader3;
    private Post post;

    @Test
    void common_highlight_proposal_after_threshold() {
        String anchor = "{\"start\":10,\"end\":30,\"prefix\":\"\",\"suffix\":\"\"}";
        String passage = "shared passage text";
        for (User u : new User[] { reader, reader2, reader3 }) {
            TestHttp.authenticated(u)
                    .contentType("application/x-www-form-urlencoded")
                    .formParam("passage", passage)
                    .formParam("anchorJson", anchor)
                    .post("/forms/posts/" + post.getId() + "/highlights")
                    .then()
                    .statusCode(200);
        }

        var proposals = proposalRepository.findPendingForPost(post.getId());
        assertEquals(1, proposals.size());
        assertEquals(ProposalStatus.PENDING, proposals.getFirst().getStatus());
    }

    @Test
    void official_highlight_visible_after_approval() {
        String anchor = "{\"start\":0,\"end\":8,\"prefix\":\"\",\"suffix\":\"\"}";
        String passage = "visible!";
        for (User u : new User[] { reader, reader2, reader3 }) {
            TestHttp.authenticated(u)
                    .contentType("application/x-www-form-urlencoded")
                    .formParam("passage", passage)
                    .formParam("anchorJson", anchor)
                    .post("/forms/posts/" + post.getId() + "/highlights")
                    .then()
                    .statusCode(200);
        }

        long proposalId = proposalRepository.findPendingForPost(post.getId()).getFirst().getId();
        TestHttp.authenticated(author)
                .post("/forms/posts/" + post.getId() + "/highlight-proposals/" + proposalId + "/approve")
                .then()
                .statusCode(200);

        assertTrue(officialHighlightRepository.findVisibleForPost(post.getId()).size() >= 1);

        given().get("/" + author.getUsername() + "/post/" + post.getSlug() + "/components/highlights")
               .then()
               .statusCode(200)
               .body(containsString("visible!"));
    }

    @Test
    void post_response_link_back_only_when_approved() {
        User responder = Given.user()
                              .withUsername("hl-responder")
                              .withEmail("hl-responder@test.com")
                              .withName("HL Responder")
                              .withPassword("password123")
                              .persist();
        Post responsePost = Given.post()
                                 .withAuthor(responder)
                                 .withTitle("Response Post")
                                 .withSlug("response-post")
                                 .withContent("Response body")
                                 .withPublished(true)
                                 .persist();

        PostResponse response = new PostResponse();
        response.setSourcePost(post);
        response.setResponsePost(responsePost);
        response.setResponder(responder);
        response.setLinkBackStatus(PostResponseLinkBackStatus.PENDING);
        postResponseRepository.save(response);

        given().get("/" + author.getUsername() + "/post/" + post.getSlug() + "/components/highlights")
               .then()
               .statusCode(200)
               .body(not(containsString("response-post")));

        TestHttp.authenticated(author)
                .post("/forms/post-responses/" + response.getId() + "/approve")
                .then()
                .statusCode(200);

        given().get("/" + author.getUsername() + "/post/" + post.getSlug() + "/components/highlights")
               .then()
               .statusCode(200)
               .body(containsString(responsePost.getTitle()));
    }

    @Test
    void public_note_hidden_without_official_highlight() {
        String anchor = "{\"start\":0,\"end\":4,\"prefix\":\"\",\"suffix\":\"\"}";
        TestHttp.authenticated(reader)
                .contentType("application/x-www-form-urlencoded")
                .formParam("passage", "note")
                .formParam("anchorJson", anchor)
                .post("/forms/posts/" + post.getId() + "/highlights")
                .then()
                .statusCode(200);

        long highlightId = highlightRepository.findByPostForUser(post.getId(), reader.getId()).getFirst().getId();
        TestHttp.authenticated(reader)
                .contentType("application/x-www-form-urlencoded")
                .formParam("body", "My public thought")
                .formParam("makePublic", true)
                .post("/forms/highlights/" + highlightId + "/notes")
                .then()
                .statusCode(200);

        given().get("/" + author.getUsername() + "/post/" + post.getSlug() + "/components/highlights")
               .then()
               .statusCode(200)
               .body(not(containsString("My public thought")));
    }

    @Test
    void reader_can_create_highlight() {
        String anchor = "{\"start\":0,\"end\":5,\"prefix\":\"\",\"suffix\":\"\"}";
        TestHttp.authenticated(reader)
                .contentType("application/x-www-form-urlencoded")
                .formParam("passage", "hello")
                .formParam("anchorJson", anchor)
                .post("/forms/posts/" + post.getId() + "/highlights")
                .then()
                .statusCode(200);

        assertEquals(1, highlightRepository.countByUserAndPost(reader.getId(), post.getId()));
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        author = Given.user()
                      .withUsername("hl-author")
                      .withEmail("hl-author@test.com")
                      .withName("HL Author")
                      .withPassword("password123")
                      .persist();
        reader = Given.user()
                      .withUsername("hl-reader1")
                      .withEmail("hl-reader1@test.com")
                      .withName("HL Reader 1")
                      .withPassword("password123")
                      .persist();
        reader2 = Given.user()
                       .withUsername("hl-reader2")
                       .withEmail("hl-reader2@test.com")
                       .withName("HL Reader 2")
                       .withPassword("password123")
                       .persist();
        reader3 = Given.user()
                       .withUsername("hl-reader3")
                       .withEmail("hl-reader3@test.com")
                       .withName("HL Reader 3")
                       .withPassword("password123")
                       .persist();
        post = Given.post()
                    .withAuthor(author)
                    .withTitle("Highlight Target")
                    .withSlug("highlight-target")
                    .withContent("Body with shared passage text for highlights.")
                    .withPublished(true)
                    .persist();
    }
}
