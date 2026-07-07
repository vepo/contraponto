package dev.vepo.contraponto.messaging;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MessageReportRetentionScheduler {

    private final MessageReportRetentionService retentionService;

    @Inject
    public MessageReportRetentionScheduler(MessageReportRetentionService retentionService) {
        this.retentionService = retentionService;
    }

    @Scheduled(every = "${app.messaging.reports.retention.schedule}", concurrentExecution = ConcurrentExecution.SKIP)
    void purgeExpiredReports() {
        retentionService.purgeExpired();
    }
}
