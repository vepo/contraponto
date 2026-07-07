package dev.vepo.contraponto.messaging;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class MessageReportRetentionService {

    private static final Logger logger = LoggerFactory.getLogger(MessageReportRetentionService.class);

    private final MessageReportRepository reportRepository;
    private final int retentionDays;

    public MessageReportRetentionService(MessageReportRepository reportRepository,
                                         @ConfigProperty(name = "app.messaging.reports.retention-days", defaultValue = "90") int retentionDays) {
        this.reportRepository = reportRepository;
        this.retentionDays = retentionDays;
    }

    @Transactional
    public int purgeExpired() {
        var cutoff = LocalDateTime.now(ZoneId.systemDefault()).minusDays(retentionDays);
        int deleted = reportRepository.deleteOlderThan(cutoff);
        if (deleted > 0) {
            logger.info("Purged {} message reports older than {} days", deleted, retentionDays);
        }
        return deleted;
    }
}
