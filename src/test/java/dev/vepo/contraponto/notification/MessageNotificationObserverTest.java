package dev.vepo.contraponto.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.messaging.MessageComposeService;
import dev.vepo.contraponto.messaging.MessageThread;
import dev.vepo.contraponto.messaging.MessageThreadPaths;
import dev.vepo.contraponto.messaging.MessageThreadService;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.user.User;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class MessageNotificationObserverTest {

    private static final String PASSWORD = "password123";

    @Inject
    MessageComposeService composeService;

    @Inject
    MessageThreadService threadService;

    @Inject
    NotificationRepository notificationRepository;

    private User alice;
    private User bob;

    @Test
    void newThreadAndReply_createMessageNotificationsWithThreadLink() {
        MessageThread thread = composeService.compose(alice.getId(), bob.getUsername(), "Question", "Hi Bob");
        threadService.reply(thread.getId(), bob.getId(), "Hi Alice");

        var notifications = notificationRepository.findUnreadRecent(bob.getId(), 5);
        assertThat(notifications).anyMatch(n -> n.getType() == NotificationType.NEW_MESSAGE_THREAD
                && n.getMessageThreadId() != null
                && n.getMessageThreadId().equals(thread.getId())
                && n.getMessageThread() != null
                && n.getMessageThread().getId().equals(thread.getId())
                && MessageThreadPaths.thread(thread.getId()).equals(n.getType().linkUrl(n)));

        var aliceNotifications = notificationRepository.findUnreadRecent(alice.getId(), 5);
        assertThat(aliceNotifications).anyMatch(n -> n.getType() == NotificationType.NEW_THREAD_MESSAGE);
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        alice = Given.user()
                     .withUsername("notify-alice")
                     .withEmail("notify-alice@test.com")
                     .withName("Notify Alice")
                     .withPassword(PASSWORD)
                     .persist();
        bob = Given.user()
                   .withUsername("notify-bob")
                   .withEmail("notify-bob@test.com")
                   .withName("Notify Bob")
                   .withPassword(PASSWORD)
                   .persist();
    }
}
