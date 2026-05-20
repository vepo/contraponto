package dev.vepo.contraponto.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusIntegrationTest
class PostWriteServiceTest {

    @Inject
    PostWriteService postWriteService;

    private LoggedUser alice;
    private LoggedUser bob;
    private Blog aliceBlog;
    private Blog bobBlog;
    private Post alicePost;

    @Test
    void allowsAuthorToUpdateOwnPost() {
        Post resolved = postWriteService.resolvePostForWrite(alicePost.getId(), aliceBlog, alice);
        assertThat(resolved.getId()).isEqualTo(alicePost.getId());
    }

    @Test
    void rejectsForeignPostIdForAnotherAuthor() {
        assertThatThrownBy(() -> postWriteService.resolvePostForWrite(alicePost.getId(), bobBlog, bob))
                                                                                                       .isInstanceOf(NotFoundException.class);
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        var aliceUser = Given.user()
                             .withUsername("alice-idor")
                             .withEmail("alice-idor@test.com")
                             .withName("Alice")
                             .withPassword("Password123!")
                             .persist();
        var bobUser = Given.user()
                           .withUsername("bob-idor")
                           .withEmail("bob-idor@test.com")
                           .withName("Bob")
                           .withPassword("Password123!")
                           .persist();
        aliceBlog = aliceUser.getDefaultBlog();
        bobBlog = bobUser.getDefaultBlog();
        alice = new LoggedUser(aliceUser, "alice-session");
        bob = new LoggedUser(bobUser, "bob-session");
        alicePost = Given.post()
                         .withAuthor(aliceUser)
                         .withBlog(aliceBlog)
                         .withTitle("Alice draft")
                         .withSlug("alice-draft")
                         .withContent("body")
                         .withPublished(false)
                         .persist();
    }
}
