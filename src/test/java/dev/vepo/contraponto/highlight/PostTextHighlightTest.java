package dev.vepo.contraponto.highlight;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.postresponse.PostResponse;
import dev.vepo.contraponto.postresponse.PostResponseLinkBackStatus;
import dev.vepo.contraponto.postresponse.PostResponseRepository;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.TestHttp;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

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

    @Inject
    HighlightNoteRepository noteRepository;

    @Inject
    PostRepository postRepository;

    @Inject
    EntityManager entityManager;

    private User author;
    private User reader;
    private User reader2;
    private User reader3;
    private Post post;

    @Test
    void approved_public_note_visible_with_official_highlight() {
        String anchor = "{\"start\":0,\"end\":8,\"prefix\":\"\",\"suffix\":\"\"}";
        String passage = "official!";
        for (User u : new User[] { reader, reader2, reader3 }) {
            createHighlight(u, passage, anchor);
        }

        long proposalId = proposalRepository.findPendingForPost(post.getId()).getFirst().getId();
        TestHttp.authenticated(author)
                .post("/forms/posts/" + post.getId() + "/highlight-proposals/" + proposalId + "/approve")
                .then()
                .statusCode(200);

        long highlightId = highlightRepository.findByPostForUser(post.getId(), reader.getId()).getFirst().getId();
        TestHttp.authenticated(reader)
                .contentType("application/x-www-form-urlencoded")
                .formParam("body", "Approved public note text")
                .formParam("makePublic", "true")
                .post("/forms/highlights/" + highlightId + "/notes")
                .then()
                .statusCode(200);

        long noteId = noteRepository.findByUserAndPost(reader.getId(), post.getId()).getFirst().getId();
        entityManager.clear();
        assertEquals(HighlightNoteStatus.PENDING, noteRepository.findById(noteId).orElseThrow().getStatus());

        TestHttp.authenticated(author)
                .post("/forms/highlight-notes/" + noteId + "/approve")
                .then()
                .statusCode(200);

        entityManager.clear();
        assertEquals(HighlightNoteStatus.APPROVED, noteRepository.findById(noteId).orElseThrow().getStatus());

        given().get("/" + author.getUsername() + "/post/" + post.getSlug() + "/components/highlights")
               .then()
               .statusCode(200)
               .body(containsString("Approved public note text"));
    }

    @Test
    void author_cannot_highlight_own_post() {
        String anchor = "{\"start\":0,\"end\":5,\"prefix\":\"\",\"suffix\":\"\"}";
        TestHttp.authenticated(author)
                .contentType("application/x-www-form-urlencoded")
                .formParam("passage", "own post")
                .formParam("anchorJson", anchor)
                .post("/forms/posts/" + post.getId() + "/highlights")
                .then()
                .statusCode(400);

        assertEquals(0, highlightRepository.countByUserAndPost(author.getId(), post.getId()));
    }

    @Test
    void cannot_approve_private_note() {
        String anchor = "{\"start\":0,\"end\":4,\"prefix\":\"\",\"suffix\":\"\"}";
        createHighlight(reader, "priv", anchor);
        long highlightId = highlightRepository.findByPostForUser(post.getId(), reader.getId()).getFirst().getId();
        TestHttp.authenticated(reader)
                .contentType("application/x-www-form-urlencoded")
                .formParam("body", "Private only")
                .post("/forms/highlights/" + highlightId + "/notes")
                .then()
                .statusCode(200);

        long noteId = noteRepository.findByUserAndPost(reader.getId(), post.getId()).getFirst().getId();
        TestHttp.authenticated(author)
                .post("/forms/highlight-notes/" + noteId + "/approve")
                .then()
                .statusCode(400);

        entityManager.clear();
        assertEquals(HighlightNoteStatus.PRIVATE, noteRepository.findById(noteId).orElseThrow().getStatus());
    }

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

    private void createHighlight(User user, String passage, String anchor) {
        TestHttp.authenticated(user)
                .contentType("application/x-www-form-urlencoded")
                .formParam("passage", passage)
                .formParam("anchorJson", anchor)
                .post("/forms/posts/" + post.getId() + "/highlights")
                .then()
                .statusCode(200);
    }

    @Test
    void create_highlight_returns_highlight_id_header() {
        String anchor = "{\"start\":0,\"end\":5,\"prefix\":\"\",\"suffix\":\"\"}";
        TestHttp.authenticated(reader)
                .contentType("application/x-www-form-urlencoded")
                .formParam("passage", "hello")
                .formParam("anchorJson", anchor)
                .post("/forms/posts/" + post.getId() + "/highlights")
                .then()
                .statusCode(200)
                .header("X-Highlight-Id", notNullValue());
    }

    @Test
    void duplicate_cluster_highlight_returns_bad_request() {
        String anchor = "{\"start\":0,\"end\":5,\"prefix\":\"\",\"suffix\":\"\"}";
        createHighlight(reader, "dup", anchor);
        TestHttp.authenticated(reader)
                .contentType("application/x-www-form-urlencoded")
                .formParam("passage", "dup")
                .formParam("anchorJson", anchor)
                .post("/forms/posts/" + post.getId() + "/highlights")
                .then()
                .statusCode(400);

        assertEquals(1, highlightRepository.countByUserAndPost(reader.getId(), post.getId()));
    }

    @Test
    void empty_passage_returns_bad_request() {
        String anchor = "{\"start\":0,\"end\":5,\"prefix\":\"\",\"suffix\":\"\"}";
        TestHttp.authenticated(reader)
                .contentType("application/x-www-form-urlencoded")
                .formParam("passage", "   ")
                .formParam("anchorJson", anchor)
                .post("/forms/posts/" + post.getId() + "/highlights")
                .then()
                .statusCode(400);
    }

    @Test
    void highlight_note_modal_returns_form() {
        String anchor = "{\"start\":0,\"end\":5,\"prefix\":\"\",\"suffix\":\"\"}";
        TestHttp.authenticated(reader)
                .contentType("application/x-www-form-urlencoded")
                .formParam("passage", "hello")
                .formParam("anchorJson", anchor)
                .post("/forms/posts/" + post.getId() + "/highlights")
                .then()
                .statusCode(200);

        long highlightId = highlightRepository.findByPostForUser(post.getId(), reader.getId()).getFirst().getId();
        TestHttp.authenticated(reader)
                .get("/forms/highlights/" + highlightId + "/notes/modal")
                .then()
                .statusCode(200)
                .body(containsString("highlightNoteDialog"));
    }

    @Test
    void long_note_body_excerpt_ends_with_ellipsis_in_note_card() {
        String anchor = "{\"start\":0,\"end\":5,\"prefix\":\"\",\"suffix\":\"\"}";
        createHighlight(reader, "short", anchor);
        long highlightId = highlightRepository.findByPostForUser(post.getId(), reader.getId()).getFirst().getId();
        String longBody = "b".repeat(250);
        TestHttp.authenticated(reader)
                .contentType("application/x-www-form-urlencoded")
                .formParam("body", longBody)
                .post("/forms/highlights/" + highlightId + "/notes")
                .then()
                .statusCode(200);

        String expectedPrefix = "b".repeat(199);
        TestHttp.authenticated(reader)
                .get("/" + author.getUsername() + "/post/" + post.getSlug() + "/components/highlights")
                .then()
                .statusCode(200)
                .body(containsString(expectedPrefix + "…"));
    }

    @Test
    void long_passage_appears_in_highlights_json_for_reader() {
        String anchor = "{\"start\":0,\"end\":5,\"prefix\":\"\",\"suffix\":\"\"}";
        String longPassage = "a".repeat(150);
        createHighlight(reader, longPassage, anchor);

        TestHttp.authenticated(reader)
                .get("/" + author.getUsername() + "/post/" + post.getSlug() + "/components/highlights")
                .then()
                .statusCode(200)
                .body(containsString(longPassage));
    }

    @Test
    void non_owner_cannot_reject_proposal() {
        String anchor = "{\"start\":0,\"end\":5,\"prefix\":\"\",\"suffix\":\"\"}";
        createHighlight(reader, "gate", anchor);
        createHighlight(reader2, "gate", anchor);
        createHighlight(reader3, "gate", anchor);

        long proposalId = proposalRepository.findPendingForPost(post.getId()).getFirst().getId();
        TestHttp.authenticated(reader)
                .post("/forms/posts/" + post.getId() + "/highlight-proposals/" + proposalId + "/reject")
                .then()
                .statusCode(403);
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
    void passage_over_max_length_returns_bad_request() {
        String anchor = "{\"start\":0,\"end\":5,\"prefix\":\"\",\"suffix\":\"\"}";
        String longPassage = "x".repeat(501);
        TestHttp.authenticated(reader)
                .contentType("application/x-www-form-urlencoded")
                .formParam("passage", longPassage)
                .formParam("anchorJson", anchor)
                .post("/forms/posts/" + post.getId() + "/highlights")
                .then()
                .statusCode(400);
    }

    private PostResponse persistPendingResponse(String responderUsername, String responseSlug, String responseTitle) {
        User responder = Given.user()
                              .withUsername(responderUsername)
                              .withEmail(responseSlug + "@hl.test.com")
                              .withName("Responder")
                              .withPassword("password123")
                              .persist();
        Post responsePost = Given.post()
                                 .withAuthor(responder)
                                 .withTitle(responseTitle)
                                 .withSlug(responseSlug)
                                 .withContent("Response body")
                                 .withPublished(true)
                                 .persist();
        PostResponse response = new PostResponse();
        response.setSourcePost(postRepository.findById(post.getId()).orElseThrow());
        response.setResponsePost(postRepository.findById(responsePost.getId()).orElseThrow());
        response.setResponder(responder);
        response.setLinkBackStatus(PostResponseLinkBackStatus.PENDING);
        return postResponseRepository.save(response);
    }

    @Test
    void post_response_link_back_only_when_approved() {
        PostResponse response = persistPendingResponse("hlresp1", "response-post", "Response Post");

        given().get("/" + author.getUsername() + "/post/" + post.getSlug() + "/components/highlights")
               .then()
               .statusCode(200)
               .body(not(containsString("Response Post")));

        TestHttp.authenticated(author)
                .post("/forms/post-responses/" + response.getId() + "/approve")
                .then()
                .statusCode(200);

        given().get("/" + author.getUsername() + "/post/" + post.getSlug() + "/components/highlights")
               .then()
               .statusCode(200)
               .body(containsString("Response Post"));
    }

    @Test
    void post_response_reject_hides_link_back() {
        PostResponse response = persistPendingResponse("hlresp2", "reject-response", "Reject Response Title");
        TestHttp.authenticated(author)
                .post("/forms/post-responses/" + response.getId() + "/reject")
                .then()
                .statusCode(200);

        entityManager.clear();
        var rejected = postResponseRepository.findById(response.getId()).orElseThrow();
        assertEquals(PostResponseLinkBackStatus.REJECTED, rejected.getLinkBackStatus());
        assertThat(rejected.getResolvedAt()).isNotNull();

        given().get("/" + author.getUsername() + "/post/" + post.getSlug() + "/components/highlights")
               .then()
               .statusCode(200)
               .body(not(containsString("Reject Response Title")));
    }

    @Test
    void post_response_revoke_after_approve_hides_link_back() {
        PostResponse response = persistPendingResponse("hlresp3", "revoke-response", "Revoke Response");
        TestHttp.authenticated(author)
                .post("/forms/post-responses/" + response.getId() + "/approve")
                .then()
                .statusCode(200);

        given().get("/" + author.getUsername() + "/post/" + post.getSlug() + "/components/highlights")
               .then()
               .statusCode(200)
               .body(containsString("Revoke Response"));

        TestHttp.authenticated(author)
                .post("/forms/post-responses/" + response.getId() + "/revoke")
                .then()
                .statusCode(200);

        assertEquals(PostResponseLinkBackStatus.REVOKED,
                     postResponseRepository.findById(response.getId()).orElseThrow().getLinkBackStatus());

        given().get("/" + author.getUsername() + "/post/" + post.getSlug() + "/components/highlights")
               .then()
               .statusCode(200)
               .body(not(containsString("Revoke Response")));
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

    @Test
    void reader_can_remove_own_highlight() {
        String anchor = "{\"start\":0,\"end\":5,\"prefix\":\"\",\"suffix\":\"\"}";
        TestHttp.authenticated(reader)
                .contentType("application/x-www-form-urlencoded")
                .formParam("passage", "hello")
                .formParam("anchorJson", anchor)
                .post("/forms/posts/" + post.getId() + "/highlights")
                .then()
                .statusCode(200);

        long highlightId = highlightRepository.findByPostForUser(post.getId(), reader.getId()).getFirst().getId();
        TestHttp.authenticated(reader)
                .delete("/forms/posts/" + post.getId() + "/highlights/" + highlightId)
                .then()
                .statusCode(200);

        assertEquals(0, highlightRepository.countByUserAndPost(reader.getId(), post.getId()));
    }

    @Test
    void reader_can_remove_own_note() {
        String anchor = "{\"start\":0,\"end\":5,\"prefix\":\"\",\"suffix\":\"\"}";
        TestHttp.authenticated(reader)
                .contentType("application/x-www-form-urlencoded")
                .formParam("passage", "hello")
                .formParam("anchorJson", anchor)
                .post("/forms/posts/" + post.getId() + "/highlights")
                .then()
                .statusCode(200);

        long highlightId = highlightRepository.findByPostForUser(post.getId(), reader.getId()).getFirst().getId();
        TestHttp.authenticated(reader)
                .contentType("application/x-www-form-urlencoded")
                .formParam("body", "Temporary note")
                .post("/forms/highlights/" + highlightId + "/notes")
                .then()
                .statusCode(200);

        long noteId = noteRepository.findByUserAndPost(reader.getId(), post.getId()).getFirst().getId();
        TestHttp.authenticated(reader)
                .delete("/forms/highlights/" + highlightId + "/notes/" + noteId)
                .then()
                .statusCode(200);

        assertTrue(noteRepository.findByUserAndPost(reader.getId(), post.getId()).isEmpty());
    }

    @Test
    void reject_proposal_prevents_official_highlight() {
        String anchor = "{\"start\":10,\"end\":30,\"prefix\":\"\",\"suffix\":\"\"}";
        String passage = "reject me cluster";
        for (User u : new User[] { reader, reader2, reader3 }) {
            createHighlight(u, passage, anchor);
        }

        long proposalId = proposalRepository.findPendingForPost(post.getId()).getFirst().getId();
        TestHttp.authenticated(author)
                .post("/forms/posts/" + post.getId() + "/highlight-proposals/" + proposalId + "/reject")
                .then()
                .statusCode(200);

        entityManager.clear();
        var proposal = proposalRepository.findById(proposalId).orElseThrow();
        assertEquals(ProposalStatus.REJECTED, proposal.getStatus());
        assertThat(proposal.getResolvedAt()).isNotNull();
        assertTrue(officialHighlightRepository.findVisibleForPost(post.getId()).isEmpty());

        given().get("/" + author.getUsername() + "/post/" + post.getSlug() + "/components/highlights")
               .then()
               .statusCode(200)
               .body(not(containsString("reject me cluster")));
    }

    @Test
    void reject_public_note_hides_from_post() {
        String anchor = "{\"start\":0,\"end\":6,\"prefix\":\"\",\"suffix\":\"\"}";
        String passage = "reject!";
        for (User u : new User[] { reader, reader2, reader3 }) {
            createHighlight(u, passage, anchor);
        }

        long proposalId = proposalRepository.findPendingForPost(post.getId()).getFirst().getId();
        TestHttp.authenticated(author)
                .post("/forms/posts/" + post.getId() + "/highlight-proposals/" + proposalId + "/approve")
                .then()
                .statusCode(200);

        long highlightId = highlightRepository.findByPostForUser(post.getId(), reader.getId()).getFirst().getId();
        TestHttp.authenticated(reader)
                .contentType("application/x-www-form-urlencoded")
                .formParam("body", "Note to reject publicly")
                .formParam("makePublic", "true")
                .post("/forms/highlights/" + highlightId + "/notes")
                .then()
                .statusCode(200);

        long noteId = noteRepository.findByUserAndPost(reader.getId(), post.getId()).getFirst().getId();
        TestHttp.authenticated(author)
                .post("/forms/highlight-notes/" + noteId + "/reject")
                .then()
                .statusCode(200);

        entityManager.clear();
        assertEquals(HighlightNoteStatus.REJECTED, noteRepository.findById(noteId).orElseThrow().getStatus());

        given().get("/" + author.getUsername() + "/post/" + post.getSlug() + "/components/highlights")
               .then()
               .statusCode(200)
               .body(not(containsString("Note to reject publicly")));
    }

    @Test
    void saved_private_note_shows_owner_and_timestamp() {
        String anchor = "{\"start\":0,\"end\":5,\"prefix\":\"\",\"suffix\":\"\"}";
        TestHttp.authenticated(reader)
                .contentType("application/x-www-form-urlencoded")
                .formParam("passage", "hello")
                .formParam("anchorJson", anchor)
                .post("/forms/posts/" + post.getId() + "/highlights")
                .then()
                .statusCode(200);

        long highlightId = highlightRepository.findByPostForUser(post.getId(), reader.getId()).getFirst().getId();
        TestHttp.authenticated(reader)
                .contentType("application/x-www-form-urlencoded")
                .formParam("body", "My private note on this passage")
                .post("/forms/highlights/" + highlightId + "/notes")
                .then()
                .statusCode(200);

        TestHttp.authenticated(reader)
                .get("/" + author.getUsername() + "/post/" + post.getSlug() + "/components/highlights")
                .then()
                .statusCode(200)
                .body(containsString("My private note on this passage"))
                .body(containsString("HL Reader 1"))
                .body(containsString("highlight-note-card"))
                .body(containsString("post-highlight-notes"));
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
