package dev.vepo.contraponto.notification;

import java.net.URI;

import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Logged
@ApplicationScoped
@Path("/forms/notifications/read")
public class MarkNotificationsReadEndpoint {

    private final NotificationRepository notificationRepository;
    private final LoggedUser loggedUser;

    @Inject
    public MarkNotificationsReadEndpoint(NotificationRepository notificationRepository, LoggedUser loggedUser) {
        this.notificationRepository = notificationRepository;
        this.loggedUser = loggedUser;
    }

    @POST
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    public Response markAllRead() {
        notificationRepository.markAllRead(loggedUser.getId());
        return Response.seeOther(URI.create("/notifications")).build();
    }
}
