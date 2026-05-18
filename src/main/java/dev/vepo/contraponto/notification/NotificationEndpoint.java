package dev.vepo.contraponto.notification;

import dev.vepo.contraponto.custompage.CustomPageRepository;
import dev.vepo.contraponto.custompage.Links;
import dev.vepo.contraponto.navigation.BreadcrumbService;
import dev.vepo.contraponto.navigation.BreadcrumbTrail;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

@Logged
@ApplicationScoped
@Path("/notifications")
public class NotificationEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance notifications(LoggedUser user,
                                                            Links links,
                                                            Page<Notification> notifications,
                                                            long unreadCount,
                                                            BreadcrumbTrail breadcrumb);

        public static native TemplateInstance panel(Page<Notification> notifications,
                                                    long unreadCount,
                                                    String basePath);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final NotificationRepository notificationRepository;
    private final CustomPageRepository customPageRepository;
    private final LoggedUser loggedUser;
    private final BreadcrumbService breadcrumbService;

    @Inject
    public NotificationEndpoint(NotificationRepository notificationRepository,
                                CustomPageRepository customPageRepository,
                                LoggedUser loggedUser,
                                BreadcrumbService breadcrumbService) {
        this.notificationRepository = notificationRepository;
        this.customPageRepository = customPageRepository;
        this.loggedUser = loggedUser;
        this.breadcrumbService = breadcrumbService;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response list(@QueryParam("page") @DefaultValue("1") int page) {
        return Response.seeOther(UriBuilder.fromPath("/account/notifications").queryParam("page", page).build()).build();
    }

    public TemplateInstance renderHubPanel(int page, String basePath) {
        long userId = loggedUser.getId();
        var notifications = notificationRepository.findPage(userId, PageQuery.forGrid(20, page));
        long unreadCount = notificationRepository.countUnread(userId);
        return Templates.panel(notifications, unreadCount, basePath);
    }
}
