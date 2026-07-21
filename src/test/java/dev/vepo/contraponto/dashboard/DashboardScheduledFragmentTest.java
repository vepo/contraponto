package dev.vepo.contraponto.dashboard;

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
class DashboardScheduledFragmentTest {

    private User alice;
    private User bob;
    private Blog aliceBlog;
    private Post aliceScheduled;

    @BeforeEach
    void setUp() {
        Given.cleanup();
        alice = Given.user()
                     .withUsername("alice-dashboard")
                     .withEmail("alice-dashboard@test.com")
                     .withName("Alice")
                     .withPassword("Password123!")
                     .persist();
        bob = Given.user()
                   .withUsername("bob-dashboard")
                   .withEmail("bob-dashboard@test.com")
                   .withName("Bob")
                   .withPassword("Password123!")
                   .persist();
        aliceBlog = alice.getDefaultBlog();
        aliceScheduled = Given.post()
                              .withAuthor(alice)
                              .withBlog(aliceBlog)
                              .withTitle("Alice Upcoming Post")
                              .withSlug("dashboard-scheduled-post")
                              .withContent("body")
                              .withPublished(false)
                              .withScheduledAt(LocalDateTime.now().plusDays(1))
                              .persist();
    }

    @Test
    void showsOnlyLoggedInAuthorsOwnScheduledPosts() {
        TestHttp.authenticated(alice).when()
                .get("/manage/dashboard/components/scheduled")
                .then()
                .statusCode(200)
                .body(containsString(aliceScheduled.getTitle()));

        TestHttp.authenticated(bob).when()
                .get("/manage/dashboard/components/scheduled")
                .then()
                .statusCode(200)
                .body(not(containsString(aliceScheduled.getTitle())));
    }
}
