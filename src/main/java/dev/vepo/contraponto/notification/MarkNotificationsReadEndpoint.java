package dev.vepo.contraponto.notification;

import dev.vepo.contraponto.shared.htmx.HtmxTriggers;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.user.LoggedUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Logged
@ApplicationScoped
@Path("/forms/notifications/read")
public class MarkNotificationsReadEndpoint {

    private static final String HTMX_REQUEST_HEADER = "HX-Request";
    private static final String HTMX_TARGET_HEADER = "HX-Target";

    private static boolean targetsOverlay(String hxTarget) {
        return hxTarget != null && hxTarget.contains("notificationOverlay");
    }

    private final NotificationRepository notificationRepository;
    private final NotificationOverlayService notificationOverlayService;
    private final NotificationEndpoint notificationEndpoint;

    private final LoggedUser loggedUser;

    @Inject
    public MarkNotificationsReadEndpoint(NotificationRepository notificationRepository,
                                         NotificationOverlayService notificationOverlayService,
                                         NotificationEndpoint notificationEndpoint,
                                         LoggedUser loggedUser) {
        this.notificationRepository = notificationRepository;
        this.notificationOverlayService = notificationOverlayService;
        this.notificationEndpoint = notificationEndpoint;
        this.loggedUser = loggedUser;
    }

    @POST
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    public Response markAllRead(@Context HttpHeaders headers) {
        notificationRepository.markAllRead(loggedUser.getId());

        if (!"true".equalsIgnoreCase(headers.getHeaderString(HTMX_REQUEST_HEADER))) {
            return Response.seeOther(java.net.URI.create("/account/notifications")).build();
        }

        String html;
        if (targetsOverlay(headers.getHeaderString(HTMX_TARGET_HEADER))) {
            html = notificationOverlayService.render(loggedUser).render();
        } else {
            html = notificationEndpoint.renderHubPanel(1, "/account/notifications").render();
        }

        return Response.ok(html)
                       .header(HtmxTriggers.HEADER_AFTER_SETTLE, HtmxTriggers.NOTIFICATIONS_CHANGED_ON_BODY)
                       .build();
    }
}
