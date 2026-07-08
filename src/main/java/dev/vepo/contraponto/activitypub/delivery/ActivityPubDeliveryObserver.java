package dev.vepo.contraponto.activitypub.delivery;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import dev.vepo.contraponto.activitypub.ActivityPubSettings;
import dev.vepo.contraponto.activitypub.actor.ActivityPubActor;
import dev.vepo.contraponto.activitypub.actor.ActivityPubActorRepository;
import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.notification.PostPublishedEvent;
import dev.vepo.contraponto.notification.PostUnpublishedEvent;

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

    @Transactional
    void afterPublish(@Observes(during = TransactionPhase.AFTER_SUCCESS) PostPublishedEvent event) {
        if (!settings.enabled()) {
            return;
        }
        var blog = blogRepository.findById(event.blogId()).orElse(null);
        if (blog == null || !blog.isActive()) {
            return;
        }
        actorRepository.findByUserId(event.authorUserId())
                       .filter(ActivityPubActor::isFederationEnabled)
                       .ifPresent(actor -> deliveryService.enqueueCreateForPublishedPost(event.postId(), actor));
    }

    @Transactional
    void afterUnpublish(@Observes(during = TransactionPhase.AFTER_SUCCESS) PostUnpublishedEvent event) {
        if (!settings.enabled()) {
            return;
        }
        var blog = blogRepository.findById(event.blogId()).orElse(null);
        if (blog == null || !blog.isActive()) {
            return;
        }
        actorRepository.findByUserId(event.authorUserId())
                       .filter(ActivityPubActor::isFederationEnabled)
                       .ifPresent(actor -> deliveryService.enqueueDeleteForUnpublishedPost(event.postId(), actor));
    }
}
