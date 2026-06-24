package dev.vepo.contraponto.notification;

import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.user.LoggedUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Logged
@ApplicationScoped
@Path("/components/notifications/overlay")
public class NotificationOverlayEndpoint {

    private final NotificationOverlayService notificationOverlayService;
    private final LoggedUser loggedUser;

    @Inject
    public NotificationOverlayEndpoint(NotificationOverlayService notificationOverlayService, LoggedUser loggedUser) {
        this.notificationOverlayService = notificationOverlayService;
        this.loggedUser = loggedUser;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response overlay() {
        return Response.ok(notificationOverlayService.render(loggedUser)).build();
    }
}
