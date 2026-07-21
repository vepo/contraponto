package dev.vepo.contraponto.library;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.shared.TestHttp;
import dev.vepo.contraponto.user.User;

@QuarkusIntegrationTest
class LibraryEndpointTest {

    private User alice;
    private Blog aliceBlog;
    private Post scheduled;
    private Post plainDraft;

    @Test
    void scheduledTabListsOnlyScheduledPosts() {
        TestHttp.authenticated(alice).when()
                .get("/writing/library/components/tab/scheduled")
                .then()
                .statusCode(200)
                .body(containsString(scheduled.getTitle()))
                .body(not(containsString(plainDraft.getTitle())));
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        alice = Given.user()
                     .withUsername("alice-library")
                     .withEmail("alice-library@test.com")
                     .withName("Alice")
                     .withPassword("Password123!")
                     .persist();
        aliceBlog = alice.getDefaultBlog();
        scheduled = Given.post()
                         .withAuthor(alice)
                         .withBlog(aliceBlog)
                         .withTitle("Scheduled Library Post")
                         .withSlug("library-scheduled-post")
                         .withContent("body")
                         .withPublished(false)
                         .withScheduledAt(LocalDateTime.now().plusDays(1))
                         .persist();
        plainDraft = Given.post()
                          .withAuthor(alice)
                          .withBlog(aliceBlog)
                          .withTitle("Plain Draft")
                          .withSlug("library-plain-draft")
                          .withContent("body")
                          .withPublished(false)
                          .persist();
    }
}
