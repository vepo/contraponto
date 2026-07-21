package dev.vepo.contraponto.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.user.User;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

@QuarkusIntegrationTest
class PostScheduleServiceTest {

    @Inject
    PostScheduleService scheduleService;

    @Inject
    PostPublicationService publicationService;

    private User author;

    @Test
    void cancelClearsScheduledAtAndPostBehavesAsDraft() {
        var post = Given.post()
                        .withAuthor(author)
                        .withTitle("Cancel me")
                        .withContent("Body")
                        .withSlug("cancel-me")
                        .withPublished(false)
                        .withScheduledAt(scheduleService.earliestAllowed().plusDays(1))
                        .persist();

        scheduleService.cancel(post);

        assertThat(post.getScheduledAt()).isNull();
        assertThat(post.isPublished()).isFalse();
        assertThat(post.isScheduled()).isFalse();
    }

    @Test
    void cancelOnNonScheduledPostThrows() {
        var post = Given.post()
                        .withAuthor(author)
                        .withTitle("Plain draft")
                        .withContent("Body")
                        .withSlug("plain-draft-cancel")
                        .withPublished(false)
                        .persist();

        assertThatThrownBy(() -> scheduleService.cancel(post)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void meetsMinimumLeadTimeAcceptsFarEnoughTarget() {
        LocalDateTime farEnough = scheduleService.earliestAllowed().plusMinutes(5);
        assertThat(scheduleService.meetsMinimumLeadTime(farEnough)).isTrue();
    }

    @Test
    void meetsMinimumLeadTimeRejectsTooSoonTarget() {
        LocalDateTime tooSoon = LocalDateTime.now().plusSeconds(1);
        assertThat(scheduleService.meetsMinimumLeadTime(tooSoon)).isFalse();
    }

    @Test
    void scheduleOnAlreadyPublishedPostThrows() {
        var post = Given.post()
                        .withAuthor(author)
                        .withTitle("Live")
                        .withContent("Body")
                        .withSlug("already-published-schedule")
                        .withPublished(true)
                        .persist();

        assertThatThrownBy(() -> scheduleService.schedule(post, scheduleService.earliestAllowed().plusDays(1)))
                                                                                                               .isInstanceOf(BadRequestException.class);
    }

    @Test
    void scheduleSetsScheduledAtOnDraft() {
        var post = Given.post()
                        .withAuthor(author)
                        .withTitle("Future post")
                        .withContent("Body")
                        .withSlug("future-post")
                        .withPublished(false)
                        .persist();
        LocalDateTime target = scheduleService.earliestAllowed().plusDays(1);

        scheduleService.schedule(post, target);

        assertThat(post.getScheduledAt()).isEqualTo(target);
        assertThat(post.isScheduled()).isTrue();
        assertThat(post.isPublished()).isFalse();
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        author = Given.user()
                      .withUsername("scheduleauthor")
                      .withEmail("scheduleauthor@example.com")
                      .withPassword("pw123456789")
                      .withName("Schedule Author")
                      .persist();
    }
}
