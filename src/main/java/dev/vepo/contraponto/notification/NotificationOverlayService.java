package dev.vepo.contraponto.notification;

import java.util.List;

import dev.vepo.contraponto.shared.infra.LoggedUser;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class NotificationOverlayService {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance overlay(List<Notification> notifications,
                                                      long unreadCount);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final NotificationRepository notificationRepository;
    private final NotificationHtmxConfig notificationHtmxConfig;

    @Inject
    public NotificationOverlayService(NotificationRepository notificationRepository,
                                      NotificationHtmxConfig notificationHtmxConfig) {
        this.notificationRepository = notificationRepository;
        this.notificationHtmxConfig = notificationHtmxConfig;
    }

    public TemplateInstance render(LoggedUser loggedUser) {
        long userId = loggedUser.getId();
        var notifications = notificationRepository.findUnreadRecent(userId, notificationHtmxConfig.overlayLimit());
        long unreadCount = notificationRepository.countUnread(userId);
        return Templates.overlay(notifications, unreadCount);
    }
}
