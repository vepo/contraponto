package dev.vepo.contraponto.activitypub;

import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.notification.PostPublishedEvent;
import dev.vepo.contraponto.notification.PostUnpublishedEvent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;

@ApplicationScoped
public class ActivityPubDeliveryObserver {

    private final ActivityPubSettings settings;
    private final ActivityPubActorRepository actorRepository;
    private final BlogRepository blogRepository;
    private final ActivityPubDeliveryService deliveryService;

    @Inject
    public ActivityPubDeliveryObserver(ActivityPubSettings settings,
                                       ActivityPubActorRepository actorRepository,
                                       BlogRepository blogRepository,
                                       ActivityPubDeliveryService deliveryService) {
        this.settings = settings;
        this.actorRepository = actorRepository;
        this.blogRepository = blogRepository;
        this.deliveryService = deliveryService;
    }

    void afterPublish(@Observes(during = TransactionPhase.AFTER_SUCCESS) PostPublishedEvent event) {
        if (!settings.enabled()) {
            return;
        }
        var blog = blogRepository.findById(event.blogId()).orElse(null);
        if (blog == null || !blog.isMain()) {
            return;
        }
        actorRepository.findByUserId(event.authorUserId())
                       .filter(ActivityPubActor::isFederationEnabled)
                       .ifPresent(actor -> deliveryService.enqueueCreateForPublishedPost(event.postId(), actor));
    }

    void afterUnpublish(@Observes(during = TransactionPhase.AFTER_SUCCESS) PostUnpublishedEvent event) {
        if (!settings.enabled()) {
            return;
        }
        var blog = blogRepository.findById(event.blogId()).orElse(null);
        if (blog == null || !blog.isMain()) {
            return;
        }
        actorRepository.findByUserId(event.authorUserId())
                       .filter(ActivityPubActor::isFederationEnabled)
                       .ifPresent(actor -> deliveryService.enqueueDeleteForUnpublishedPost(event.postId(), actor));
    }
}
