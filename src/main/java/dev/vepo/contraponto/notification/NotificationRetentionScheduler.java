package dev.vepo.contraponto.notification;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class NotificationRetentionScheduler {

    private final NotificationRetentionService retentionService;

    @Inject
    public NotificationRetentionScheduler(NotificationRetentionService retentionService) {
        this.retentionService = retentionService;
    }

    @Scheduled(every = "${app.notifications.retention.schedule}", concurrentExecution = ConcurrentExecution.SKIP)
    void purgeExpiredNotifications() {
        retentionService.purgeExpired();
    }
}
