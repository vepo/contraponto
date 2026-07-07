package dev.vepo.contraponto.activitypub;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ActivityPubDeliveryScheduler {

    private final ActivityPubSettings settings;
    private final ActivityPubDeliveryService deliveryService;

    @Inject
    public ActivityPubDeliveryScheduler(ActivityPubSettings settings, ActivityPubDeliveryService deliveryService) {
        this.settings = settings;
        this.deliveryService = deliveryService;
    }

    @Scheduled(every = "30s", concurrentExecution = ConcurrentExecution.SKIP)
    void processPendingDeliveries() {
        if (!settings.enabled()) {
            return;
        }
        deliveryService.processPendingDeliveries();
    }
}
