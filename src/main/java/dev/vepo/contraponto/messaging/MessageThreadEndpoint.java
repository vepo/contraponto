package dev.vepo.contraponto.messaging;

import dev.vepo.contraponto.navigation.BreadcrumbService;
import dev.vepo.contraponto.navigation.NavigationHub;
import dev.vepo.contraponto.navigation.NavigationHubService;
import dev.vepo.contraponto.seo.SeoService;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.user.LoggedUser;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.RawString;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Logged
@Path("/account/messages")
@ApplicationScoped
public class MessageThreadEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance panel(MessageThreadView view);

        private Templates() {
            throw new UnsupportedOperationException("Utility class");
        }
    }

    private final MessageThreadService threadService;
    private final MessageThreadAccess threadAccess;
    private final LoggedUser loggedUser;
    private final BreadcrumbService breadcrumbService;
    private final NavigationHubService hubService;
    private final SeoService seoService;

    @Inject
    public MessageThreadEndpoint(MessageThreadService threadService,
                                 MessageThreadAccess threadAccess,
                                 LoggedUser loggedUser,
                                 BreadcrumbService breadcrumbService,
                                 NavigationHubService hubService,
                                 SeoService seoService) {
        this.threadService = threadService;
        this.threadAccess = threadAccess;
        this.loggedUser = loggedUser;
        this.breadcrumbService = breadcrumbService;
        this.hubService = hubService;
        this.seoService = seoService;
    }

    @GET
    @Path("{threadId:[0-9]+}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance thread(@PathParam("threadId") long threadId) {
        MessageThreadView view = threadService.loadThreadView(threadId, loggedUser.getId());
        if (!threadAccess.canParticipate(view.thread(), loggedUser)) {
            throw new NotFoundException("Thread not found.");
        }
        var breadcrumb = breadcrumbService.forMessageThread(view.thread());
        return hubService.shellWithCustomPanel(NavigationHub.ACCOUNT,
                                               "mailbox",
                                               breadcrumb,
                                               new RawString(Templates.panel(view).render()),
                                               seoService.forPrivatePage(view.thread().getTitle()));
    }
}
