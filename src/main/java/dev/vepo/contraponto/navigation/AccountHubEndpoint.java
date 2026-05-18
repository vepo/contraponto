package dev.vepo.contraponto.navigation;

import dev.vepo.contraponto.custompage.CustomPageRepository;
import dev.vepo.contraponto.custompage.Links;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Logged
@Path("/account")
@ApplicationScoped
public class AccountHubEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance hub(String pageTitle,
                                                  String title,
                                                  String subtitle,
                                                  BreadcrumbTrail breadcrumb,
                                                  java.util.List<HubSection> sections,
                                                  Links links,
                                                  LoggedUser user);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final CustomPageRepository customPageRepository;
    private final NavigationHubLinks navigationHubLinks;
    private final BreadcrumbService breadcrumbService;
    private final LoggedUser loggedUser;

    @Inject
    public AccountHubEndpoint(CustomPageRepository customPageRepository,
                              NavigationHubLinks navigationHubLinks,
                              BreadcrumbService breadcrumbService,
                              LoggedUser loggedUser) {
        this.customPageRepository = customPageRepository;
        this.navigationHubLinks = navigationHubLinks;
        this.breadcrumbService = breadcrumbService;
        this.loggedUser = loggedUser;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance hub() {
        return Templates.hub("Account",
                             "Account",
                             "Notifications, subscriptions, and profile settings.",
                             breadcrumbService.hub(NavigationHub.ACCOUNT),
                             navigationHubLinks.accountSections(),
                             customPageRepository.loadLinks(),
                             loggedUser);
    }
}
