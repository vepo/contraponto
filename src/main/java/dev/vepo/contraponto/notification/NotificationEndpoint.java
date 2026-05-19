package dev.vepo.contraponto.notification;

import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@Logged
@ApplicationScoped
public class NotificationEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance panel(Page<Notification> notifications,
                                                    long unreadCount,
                                                    String basePath);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final NotificationRepository notificationRepository;
    private final LoggedUser loggedUser;

    @Inject
    public NotificationEndpoint(NotificationRepository notificationRepository, LoggedUser loggedUser) {
        this.notificationRepository = notificationRepository;
        this.loggedUser = loggedUser;
    }

    public TemplateInstance renderHubPanel(int page, String basePath) {
        long userId = loggedUser.getId();
        var notifications = notificationRepository.findPage(userId, PageQuery.forGrid(20, page));
        long unreadCount = notificationRepository.countUnread(userId);
        return Templates.panel(notifications, unreadCount, basePath);
    }
}
