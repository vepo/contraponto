package dev.vepo.contraponto.notification;

import dev.vepo.contraponto.shared.htmx.HtmxTriggers;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.user.LoggedUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Logged
@ApplicationScoped
@Path("/forms/notifications")
public class DismissNotificationEndpoint {

    private final NotificationRepository notificationRepository;
    private final NotificationOverlayService notificationOverlayService;
    private final LoggedUser loggedUser;

    @Inject
    public DismissNotificationEndpoint(NotificationRepository notificationRepository,
                                       NotificationOverlayService notificationOverlayService,
                                       LoggedUser loggedUser) {
        this.notificationRepository = notificationRepository;
        this.notificationOverlayService = notificationOverlayService;
        this.loggedUser = loggedUser;
    }

    @POST
    @Path("{id}/dismiss")
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    public Response dismiss(@PathParam("id") long id) {
        notificationRepository.markRead(id, loggedUser.getId());
        return Response.ok(notificationOverlayService.render(loggedUser).render())
                       .header(HtmxTriggers.HEADER_AFTER_SETTLE, HtmxTriggers.NOTIFICATIONS_CHANGED_ON_BODY)
                       .build();
    }
}
