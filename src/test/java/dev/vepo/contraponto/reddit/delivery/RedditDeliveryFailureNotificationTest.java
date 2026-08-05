package dev.vepo.contraponto.reddit.delivery;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.notification.NotificationRepository;
import dev.vepo.contraponto.notification.NotificationType;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.reddit.account.UserRedditAccount;
import dev.vepo.contraponto.reddit.account.UserRedditAccountRepository;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import dev.vepo.contraponto.user.User;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class RedditDeliveryFailureNotificationTest {

    @Inject
    RedditDeliveryRepository deliveryRepository;

    @Inject
    RedditDeliveryService deliveryService;

    @Inject
    UserRedditAccountRepository accountRepository;

    @Inject
    NotificationRepository notificationRepository;

    private User user;
    private Post post;

    @Test
    void notifiesOnlyOnceRetriesAreExhausted() {
        // Seed a delivery already at 4 failed attempts (undecryptable ciphertext means
        // every processing attempt fails without any network call).
        var delivery = new RedditDelivery(post, user, RedditDeliveryKind.CREATE);
        for (int i = 0; i < 4; i++) {
            delivery.markFailed("seed", null);
        }
        Given.transaction(() -> deliveryRepository.create(delivery));
        assertThat(redditSyndicationFailedCount()).isZero();

        deliveryService.processPendingDeliveries();

        assertThat(redditSyndicationFailedCount()).isOne();
        var reloaded = deliveryRepository.findPendingReady(java.time.LocalDateTime.now().plusHours(2));
        assertThat(reloaded).isEmpty(); // FAILED status is no longer "pending ready"

        // A second processing pass must not fire a duplicate notification.
        deliveryService.processPendingDeliveries();
        assertThat(redditSyndicationFailedCount()).isOne();
    }

    private long redditSyndicationFailedCount() {
        return notificationRepository.findPage(user.getId(), PageQuery.forGrid(50, 1))
                                     .data()
                                     .stream()
                                     .filter(n -> n.getType() == NotificationType.REDDIT_SYNDICATION_FAILED)
                                     .count();
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        user = Given.user()
                    .withUsername("redditfailnfy")
                    .withEmail("redditfailnotify@example.com")
                    .withPassword("pw123456789")
                    .withName("Failure User")
                    .persist();
        post = Given.post()
                    .withAuthor(user)
                    .withTitle("Failing Post")
                    .withSlug("failing-post")
                    .withDescription("Summary")
                    .withContent("Body")
                    .persist();
        Given.transaction(() -> accountRepository.create(new UserRedditAccount(user, "coolredditor", "not-valid-ciphertext")));
    }
}
