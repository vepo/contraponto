package dev.vepo.contraponto.components.forms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.post.PostScheduleService;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.shared.TestHttp;
import dev.vepo.contraponto.user.User;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class ScheduleEndpointTest {

    @Inject
    PostRepository postRepository;

    @Inject
    PostScheduleService scheduleService;

    private User alice;
    private User bob;
    private Blog aliceBlog;
    private Post draft;
    private Post scheduled;

    @Test
    void cancelClearsSchedule() {
        TestHttp.authenticated(alice).when()
                .post("/forms/write/schedule/cancel/" + scheduled.getId())
                .then()
                .statusCode(200)
                .header("X-Toast-I18n-Key", equalTo("toast.post.scheduleCancelled"));

        var reloaded = postRepository.findById(scheduled.getId()).orElseThrow();
        assertThat(reloaded.getScheduledAt()).isNull();
    }

    @Test
    void cancelOnNonScheduledDraftReturnsBadRequest() {
        TestHttp.authenticated(alice).when()
                .post("/forms/write/schedule/cancel/" + draft.getId())
                .then()
                .statusCode(400)
                .header("X-Toast-I18n-Key", equalTo("toast.post.notScheduled"));
    }

    private String farEnoughIso() {
        return scheduleService.earliestAllowed().plusDays(1).toInstant(ZoneOffset.UTC).toString();
    }

    @Test
    void foreignUserGetsNotFoundOnCancel() {
        TestHttp.authenticated(bob).when()
                .post("/forms/write/schedule/cancel/" + scheduled.getId())
                .then()
                .statusCode(404)
                .header("X-Toast-I18n-Key", equalTo("toast.post.notFound"));
    }

    @Test
    void scheduleRejectsTooSoonTarget() {
        String tooSoon = Instant.now().plusSeconds(1).toString();

        TestHttp.authenticated(alice).contentType("application/x-www-form-urlencoded")
                .formParam("postId", draft.getId())
                .formParam("scheduledAt", tooSoon)
                .post("/forms/write/schedule")
                .then()
                .statusCode(400)
                .header("X-Toast-I18n-Key", equalTo("toast.post.scheduleInvalidTime"));
    }

    @Test
    void scheduleSetsScheduledAtOnDraft() {
        TestHttp.authenticated(alice).contentType("application/x-www-form-urlencoded")
                .formParam("postId", draft.getId())
                .formParam("scheduledAt", farEnoughIso())
                .post("/forms/write/schedule")
                .then()
                .statusCode(200)
                .header("X-Toast-I18n-Key", equalTo("toast.post.scheduled"));

        var reloaded = postRepository.findById(draft.getId()).orElseThrow();
        assertThat(reloaded.getScheduledAt()).isNotNull();
        assertThat(reloaded.isPublished()).isFalse();
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        alice = Given.user()
                     .withUsername("alice-schedule")
                     .withEmail("alice-schedule@test.com")
                     .withName("Alice")
                     .withPassword("Password123!")
                     .persist();
        bob = Given.user()
                   .withUsername("bob-schedule")
                   .withEmail("bob-schedule@test.com")
                   .withName("Bob")
                   .withPassword("Password123!")
                   .persist();
        aliceBlog = alice.getDefaultBlog();
        draft = Given.post()
                     .withAuthor(alice)
                     .withBlog(aliceBlog)
                     .withTitle("Draft")
                     .withSlug("schedule-endpoint-draft")
                     .withContent("body")
                     .withPublished(false)
                     .persist();
        scheduled = Given.post()
                         .withAuthor(alice)
                         .withBlog(aliceBlog)
                         .withTitle("Scheduled")
                         .withSlug("schedule-endpoint-scheduled")
                         .withContent("body")
                         .withPublished(false)
                         .withScheduledAt(LocalDateTime.now().plusDays(1))
                         .persist();
    }
}
