package dev.vepo.contraponto.post;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.notification.NotificationRepository;
import dev.vepo.contraponto.notification.NotificationType;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.user.User;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class PostScheduleSchedulerTest {

    @Inject
    PostScheduleScheduler scheduler;

    @Inject
    PostRepository postRepository;

    @Inject
    NotificationRepository notificationRepository;

    private User author;

    @Test
    void catchUpPublishesOverdueScheduleOnNextTick() {
        var post = Given.post()
                        .withAuthor(author)
                        .withTitle("Overdue")
                        .withContent("Body")
                        .withSlug("catch-up-overdue")
                        .withPublished(false)
                        .withScheduledAt(LocalDateTime.now().minusHours(6))
                        .persist();

        scheduler.publishDueScheduledPosts();

        var reloaded = postRepository.findById(post.getId()).orElseThrow();
        assertThat(reloaded.isPublished()).isTrue();
        assertThat(reloaded.getScheduledAt()).isNull();
    }

    @Test
    void notifiesAuthorOnSuccessfulScheduledPublish() {
        var post = Given.post()
                        .withAuthor(author)
                        .withTitle("Notify me")
                        .withContent("Body")
                        .withSlug("notify-schedule-success")
                        .withPublished(false)
                        .withScheduledAt(LocalDateTime.now().minusMinutes(1))
                        .persist();

        scheduler.publishDueScheduledPosts();

        var notifications = notificationRepository.findUnreadRecent(author.getId(), 20);
        assertThat(notifications).anyMatch(n -> n.getType() == NotificationType.SCHEDULE_PUBLISH_SUCCEEDED
                && post.getId().equals(n.getPost() != null ? n.getPost().getId() : null));
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        author = Given.user()
                      .withUsername("schedulerauthor")
                      .withEmail("schedulerauthor@example.com")
                      .withPassword("pw123456789")
                      .withName("Scheduler Author")
                      .persist();
    }

    @Test
    void skipsNotYetDuePosts() {
        var post = Given.post()
                        .withAuthor(author)
                        .withTitle("Not yet due")
                        .withContent("Body")
                        .withSlug("not-yet-due")
                        .withPublished(false)
                        .withScheduledAt(LocalDateTime.now().plusDays(1))
                        .persist();

        scheduler.publishDueScheduledPosts();

        var reloaded = postRepository.findById(post.getId()).orElseThrow();
        assertThat(reloaded.isPublished()).isFalse();
        assertThat(reloaded.getScheduledAt()).isNotNull();
    }
}
