package dev.vepo.contraponto.notification;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class NotificationRetentionService {

    public record PurgeResult(int readDeleted, int unreadDeleted) {}

    private static final Logger logger = LoggerFactory.getLogger(NotificationRetentionService.class);
    private final NotificationRepository notificationRepository;
    private final int readDays;

    private final int unreadDays;

    public NotificationRetentionService(NotificationRepository notificationRepository,
                                        @ConfigProperty(name = "app.notifications.retention.read-days", defaultValue = "7") int readDays,
                                        @ConfigProperty(name = "app.notifications.retention.unread-days", defaultValue = "30") int unreadDays) {
        this.notificationRepository = notificationRepository;
        this.readDays = readDays;
        this.unreadDays = unreadDays;
    }

    @Transactional
    public PurgeResult purgeExpired() {
        var now = LocalDateTime.now(ZoneId.systemDefault());
        int readDeleted = notificationRepository.deleteExpiredRead(now.minusDays(readDays));
        int unreadDeleted = notificationRepository.deleteExpiredUnread(now.minusDays(unreadDays));
        if (readDeleted > 0 || unreadDeleted > 0) {
            logger.info("Purged expired notifications read={} unread={}", readDeleted, unreadDeleted);
        }
        return new PurgeResult(readDeleted, unreadDeleted);
    }
}
