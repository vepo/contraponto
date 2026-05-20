package dev.vepo.contraponto.postresponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.user.User;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;

@QuarkusIntegrationTest
class PostResponseServiceTest {

    @Inject
    PostResponseService postResponseService;

    @Inject
    PostResponseRepository postResponseRepository;

    @Inject
    PostRepository postRepository;

    private User author;
    private User responder;
    private Post source;
    private Post responsePost;

    @Test
    void createOnPublish_persistsPendingResponse() {
        PostResponse created = postResponseService.createOnPublish(responsePost, source.getId(), responder.getId());

        assertEquals(PostResponseLinkBackStatus.PENDING, created.getLinkBackStatus());
        assertEquals(source.getId(), created.getSourcePost().getId());
        assertEquals(responsePost.getId(), created.getResponsePost().getId());
        assertEquals(responder.getId(), created.getResponder().getId());
    }

    @Test
    void createOnPublish_rejectsDuplicateResponse() {
        postResponseService.createOnPublish(responsePost, source.getId(), responder.getId());

        assertThrows(BadRequestException.class,
                     () -> postResponseService.createOnPublish(responsePost, source.getId(), responder.getId()));
        assertThat(postResponseRepository.findByResponsePostId(responsePost.getId())).isPresent();
    }

    @Test
    void createOnPublish_rejectsSelfResponse() {
        Post ownResponse = Given.post()
                                .withAuthor(author)
                                .withTitle("Own response attempt")
                                .withSlug("own-response")
                                .withContent("Own response body.")
                                .withPublished(true)
                                .persist();

        assertThrows(BadRequestException.class,
                     () -> postResponseService.createOnPublish(ownResponse, source.getId(), author.getId()));
        assertThat(postResponseRepository.findByResponsePostId(ownResponse.getId())).isEmpty();
    }

    @Test
    void createOnPublish_rejectsUnpublishedSource() {
        Post draftSource = Given.post()
                                .withAuthor(author)
                                .withTitle("Draft source")
                                .withSlug("draft-source")
                                .withContent("Draft source body.")
                                .withPublished(false)
                                .persist();

        assertThrows(BadRequestException.class,
                     () -> postResponseService.createOnPublish(responsePost, draftSource.getId(), responder.getId()));
    }

    @Test
    void reject_throwsWhenNotSourceOwner() {
        PostResponse existing = postResponseService.createOnPublish(responsePost, source.getId(), responder.getId());

        assertThrows(ForbiddenException.class,
                     () -> postResponseService.reject(existing.getId(), responder.getId()));
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        author = Given.user()
                      .withUsername("pr-author")
                      .withEmail("pr-author@test.com")
                      .withName("PR Author")
                      .withPassword("password123")
                      .persist();
        responder = Given.user()
                         .withUsername("pr-resp")
                         .withEmail("pr-resp@test.com")
                         .withName("PR Responder")
                         .withPassword("password123")
                         .persist();
        source = Given.post()
                      .withAuthor(author)
                      .withTitle("Source post")
                      .withSlug("source-post")
                      .withContent("Source body content.")
                      .withPublished(true)
                      .persist();
        responsePost = Given.post()
                            .withAuthor(responder)
                            .withTitle("Response post")
                            .withSlug("response-post")
                            .withContent("Response body content.")
                            .withPublished(true)
                            .persist();
    }
}
