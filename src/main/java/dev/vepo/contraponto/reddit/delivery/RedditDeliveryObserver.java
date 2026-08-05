package dev.vepo.contraponto.reddit.delivery;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.notification.PostPublishedEvent;
import dev.vepo.contraponto.notification.PostUnpublishedEvent;
import dev.vepo.contraponto.reddit.RedditSettings;
import dev.vepo.contraponto.reddit.account.UserRedditAccountRepository;

/**
 * Enqueues Reddit deliveries after publish/unpublish commits. Uses
 * {@link TxType#REQUIRES_NEW} for the same reason as
 * {@code ActivityPubDeliveryObserver}: {@link TransactionPhase#AFTER_SUCCESS}
 * runs after the publisher transaction is no longer active.
 */
@ApplicationScoped
public class RedditDeliveryObserver {

    private final RedditSettings settings;
    private final BlogRepository blogRepository;
    private final UserRedditAccountRepository accountRepository;
    private final RedditDeliveryService deliveryService;

    @Inject
    public RedditDeliveryObserver(RedditSettings settings,
                                  BlogRepository blogRepository,
                                  UserRedditAccountRepository accountRepository,
                                  RedditDeliveryService deliveryService) {
        this.settings = settings;
        this.blogRepository = blogRepository;
        this.accountRepository = accountRepository;
        this.deliveryService = deliveryService;
    }

    @Transactional(TxType.REQUIRES_NEW)
    void afterPublish(@Observes(during = TransactionPhase.AFTER_SUCCESS) PostPublishedEvent event) {
        var blog = eligibleBlog(event.blogId());
        if (blog == null) {
            return;
        }
        deliveryService.enqueueCreateForPublishedPost(event.postId(), blog.getOwner());
    }

    @Transactional(TxType.REQUIRES_NEW)
    void afterUnpublish(@Observes(during = TransactionPhase.AFTER_SUCCESS) PostUnpublishedEvent event) {
        var blog = eligibleBlog(event.blogId());
        if (blog == null) {
            return;
        }
        deliveryService.enqueueDeleteForUnpublishedPost(event.postId(), blog.getOwner());
    }

    private Blog eligibleBlog(long blogId) {
        if (!settings.enabled()) {
            return null;
        }
        var blog = blogRepository.findById(blogId).orElse(null);
        if (blog == null || !blog.isActive() || !blog.isRedditSyndicationEnabled()) {
            return null;
        }
        if (accountRepository.findByUserId(blog.getOwner().getId()).isEmpty()) {
            return null;
        }
        return blog;
    }
}
