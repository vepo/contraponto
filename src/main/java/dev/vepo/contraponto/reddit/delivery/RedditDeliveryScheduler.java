package dev.vepo.contraponto.reddit.delivery;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;

import dev.vepo.contraponto.reddit.RedditSettings;

@ApplicationScoped
public class RedditDeliveryScheduler {

    private final RedditSettings settings;
    private final RedditDeliveryService deliveryService;

    @Inject
    public RedditDeliveryScheduler(RedditSettings settings, RedditDeliveryService deliveryService) {
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
