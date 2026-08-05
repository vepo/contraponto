package dev.vepo.contraponto.reddit.delivery;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.notification.PostPublishedEvent;
import dev.vepo.contraponto.notification.PostUnpublishedEvent;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.reddit.account.RedditAccountLinkService;
import dev.vepo.contraponto.reddit.account.UserRedditAccount;
import dev.vepo.contraponto.reddit.account.UserRedditAccountRepository;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.user.User;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class RedditDeliveryObserverTest {

    @Inject
    RedditDeliveryObserver deliveryObserver;

    @Inject
    RedditDeliveryRepository deliveryRepository;

    @Inject
    BlogRepository blogRepository;

    @Inject
    UserRedditAccountRepository accountRepository;

    @Inject
    RedditAccountLinkService linkService;

    @Inject
    RedditPostRecordRepository postRecordRepository;

    private User user;
    private Post post;

    private java.util.List<RedditDelivery> deliveriesForPost() {
        return deliveryRepository.findPendingReady(LocalDateTime.now().plusMinutes(1))
                                 .stream()
                                 .filter(delivery -> delivery.getPost().getId().equals(post.getId()))
                                 .toList();
    }

    private void enableBlogToggle() {
        Given.transaction(() -> {
            var blog = blogRepository.findById(post.getBlog().getId()).orElseThrow();
            blog.setRedditSyndicationEnabled(true);
            blogRepository.save(blog);
        });
    }

    private void linkAccount() {
        Given.transaction(() -> accountRepository.create(new UserRedditAccount(user, "coolredditor", "ciphertext")));
    }

    @Test
    void publishDoesNotEnqueueWhenBlogToggleOff() {
        deliveryObserver.afterPublish(new PostPublishedEvent(post.getId(), post.getLivePublication().getId(), post.getBlog().getId(), user.getId()));

        assertThat(deliveriesForPost()).isEmpty();
    }

    @Test
    void publishDoesNotEnqueueWhenOwnerHasNoLinkedAccount() {
        enableBlogToggle();

        deliveryObserver.afterPublish(new PostPublishedEvent(post.getId(), post.getLivePublication().getId(), post.getBlog().getId(), user.getId()));

        assertThat(deliveriesForPost()).isEmpty();
    }

    @Test
    void publishEnqueuesCreateWhenToggleOnAndAccountLinked() {
        enableBlogToggle();
        linkAccount();

        deliveryObserver.afterPublish(new PostPublishedEvent(post.getId(), post.getLivePublication().getId(), post.getBlog().getId(), user.getId()));

        var deliveries = deliveriesForPost();
        assertThat(deliveries).hasSize(1);
        assertThat(deliveries.get(0).getKind()).isEqualTo(RedditDeliveryKind.CREATE);
        assertThat(deliveries.get(0).getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    void republishDoesNotEnqueueCreateWhenPostAlreadySyndicated() {
        enableBlogToggle();
        linkAccount();
        Given.transaction(() -> postRecordRepository.create(new RedditPostRecord(post, "t3_alreadysynced",
                                                                                 "https://reddit.com/r/u_coolredditor/comments/alreadysynced")));

        deliveryObserver.afterPublish(new PostPublishedEvent(post.getId(), post.getLivePublication().getId(), post.getBlog().getId(), user.getId()));

        assertThat(deliveriesForPost()).isEmpty();
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        user = Given.user()
                    .withUsername("reddelivery")
                    .withEmail("reddelivery@example.com")
                    .withPassword("pw123456789")
                    .withName("Delivery User")
                    .persist();
        post = Given.post()
                    .withAuthor(user)
                    .withTitle("Reddit Delivery Post")
                    .withSlug("reddit-delivery-post")
                    .withDescription("Summary")
                    .withContent("Body")
                    .persist();
    }

    @Test
    void unlinkCascadesDisablesBlogToggle() {
        enableBlogToggle();
        linkAccount();

        Given.transaction(() -> linkService.unlink(user));

        var reloaded = Given.transaction(() -> blogRepository.findById(post.getBlog().getId()).orElseThrow());
        assertThat(reloaded.isRedditSyndicationEnabled()).isFalse();
        assertThat(accountRepository.findByUserId(user.getId())).isEmpty();
    }

    @Test
    void unpublishEnqueuesDeleteWhenToggleOnAndAccountLinked() {
        enableBlogToggle();
        linkAccount();

        deliveryObserver.afterUnpublish(new PostUnpublishedEvent(post.getId(), post.getBlog().getId(), user.getId()));

        var deliveries = deliveriesForPost();
        assertThat(deliveries).hasSize(1);
        assertThat(deliveries.get(0).getKind()).isEqualTo(RedditDeliveryKind.DELETE);
    }
}
