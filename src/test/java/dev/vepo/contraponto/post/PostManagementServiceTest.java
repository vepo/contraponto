package dev.vepo.contraponto.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.user.User;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusIntegrationTest
class PostManagementServiceTest {

    @Inject
    PostManagementService postManagementService;

    @Inject
    PostRepository postRepository;

    private User alice;
    private User bob;
    private Blog aliceBlog;
    private LoggedUser aliceSession;
    private LoggedUser bobSession;

    @Test
    void deletePreviouslyPublishedDraftAfterUnpublish() {
        var post = Given.post()
                        .withAuthor(alice)
                        .withBlog(aliceBlog)
                        .withTitle("Was published")
                        .withSlug("was-published")
                        .withContent("body")
                        .withPublished(true)
                        .persist();

        postManagementService.unpublish(post.getId(), aliceSession);
        postManagementService.delete(post.getId(), aliceSession);

        assertThat(postRepository.findById(post.getId())).isEmpty();
    }

    @Test
    void deleteRejectsForeignUser() {
        var post = Given.post()
                        .withAuthor(alice)
                        .withBlog(aliceBlog)
                        .withTitle("Alice draft")
                        .withSlug("alice-draft-delete")
                        .withContent("body")
                        .withPublished(false)
                        .persist();

        assertThatThrownBy(() -> postManagementService.delete(post.getId(), bobSession))
                                                                                        .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteRejectsPublishedPost() {
        var post = Given.post()
                        .withAuthor(alice)
                        .withBlog(aliceBlog)
                        .withTitle("Still published")
                        .withSlug("still-published")
                        .withContent("body")
                        .withPublished(true)
                        .persist();

        assertThatThrownBy(() -> postManagementService.delete(post.getId(), aliceSession))
                                                                                          .isInstanceOf(BadRequestException.class);
    }

    @Test
    void deleteRemovesDraft() {
        var post = Given.post()
                        .withAuthor(alice)
                        .withBlog(aliceBlog)
                        .withTitle("Draft to delete")
                        .withSlug("draft-to-delete")
                        .withContent("body")
                        .withPublished(false)
                        .persist();

        postManagementService.delete(post.getId(), aliceSession);

        assertThat(postRepository.findById(post.getId())).isEmpty();
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        alice = Given.user()
                     .withUsername("alice-mgmt")
                     .withEmail("alice-mgmt@test.com")
                     .withName("Alice")
                     .withPassword("Password123!")
                     .persist();
        bob = Given.user()
                   .withUsername("bob-mgmt")
                   .withEmail("bob-mgmt@test.com")
                   .withName("Bob")
                   .withPassword("Password123!")
                   .persist();
        aliceBlog = alice.getDefaultBlog();
        aliceSession = new LoggedUser(alice, "alice-session");
        bobSession = new LoggedUser(bob, "bob-session");
    }

    @Test
    void unpublishRejectsAlreadyDraft() {
        var post = Given.post()
                        .withAuthor(alice)
                        .withBlog(aliceBlog)
                        .withTitle("Draft")
                        .withSlug("draft")
                        .withContent("body")
                        .withPublished(false)
                        .persist();

        assertThatThrownBy(() -> postManagementService.unpublish(post.getId(), aliceSession))
                                                                                             .isInstanceOf(BadRequestException.class);
    }

    @Test
    void unpublishRejectsForeignUser() {
        var post = Given.post()
                        .withAuthor(alice)
                        .withBlog(aliceBlog)
                        .withTitle("Published post")
                        .withSlug("foreign-unpublish")
                        .withContent("body")
                        .withPublished(true)
                        .persist();

        assertThatThrownBy(() -> postManagementService.unpublish(post.getId(), bobSession))
                                                                                           .isInstanceOf(NotFoundException.class);
    }

    @Test
    void unpublishSetsPublishedFalseAndClearsFeatured() {
        var post = Given.post()
                        .withAuthor(alice)
                        .withBlog(aliceBlog)
                        .withTitle("Published post")
                        .withSlug("published-post")
                        .withContent("body")
                        .withFeatured(true)
                        .withPublished(true)
                        .persist();

        postManagementService.unpublish(post.getId(), aliceSession);

        var reloaded = postRepository.findById(post.getId()).orElseThrow();
        assertThat(reloaded.isPublished()).isFalse();
        assertThat(reloaded.isFeatured()).isFalse();
        assertThat(reloaded.getLivePublication()).isNotNull();
    }
}
