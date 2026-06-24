package dev.vepo.contraponto.notification;

import dev.vepo.contraponto.user.LoggedUser;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
@Path("/components/notifications/badge")
public class NotificationBadgeEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance badge(long unreadCount, boolean authenticated);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final NotificationRepository notificationRepository;
    private final LoggedUser loggedUser;

    @Inject
    public NotificationBadgeEndpoint(NotificationRepository notificationRepository, LoggedUser loggedUser) {
        this.notificationRepository = notificationRepository;
        this.loggedUser = loggedUser;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response badge() {
        if (!loggedUser.isAuthenticated()) {
            return Response.ok(Templates.badge(0, false)).build();
        }
        long unread = notificationRepository.countUnread(loggedUser.getId());
        return Response.ok(Templates.badge(unread, true)).build();
    }
}
