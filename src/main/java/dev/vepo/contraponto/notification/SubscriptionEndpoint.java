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

@Logged
@ApplicationScoped
@Path("/subscriptions")
public class SubscriptionEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance subscriptions(LoggedUser user,
                                                            Links links,
                                                            Page<SubscriptionRow> rows,
                                                            BreadcrumbTrail breadcrumb);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final BlogAudienceRepository audienceRepository;
    private final BlogAudienceComponentEndpoint audienceComponentEndpoint;
    private final CustomPageRepository customPageRepository;
    private final LoggedUser loggedUser;
    private final BreadcrumbService breadcrumbService;

    @Inject
    public SubscriptionEndpoint(BlogAudienceRepository audienceRepository,
                                BlogAudienceComponentEndpoint audienceComponentEndpoint,
                                CustomPageRepository customPageRepository,
                                LoggedUser loggedUser,
                                BreadcrumbService breadcrumbService) {
        this.audienceRepository = audienceRepository;
        this.audienceComponentEndpoint = audienceComponentEndpoint;
        this.customPageRepository = customPageRepository;
        this.loggedUser = loggedUser;
        this.breadcrumbService = breadcrumbService;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list(@QueryParam("page") @DefaultValue("1") int page) {
        var rows = audienceRepository.findPageByUserId(loggedUser.getId(), PageQuery.forGrid(20, page))
                                     .map(a -> new SubscriptionRow(a.getBlog(),
                                                                   audienceComponentEndpoint.buildView(a.getBlog())));
        return Templates.subscriptions(loggedUser, customPageRepository.loadLinks(), rows, breadcrumbService.accountSubscriptions());
    }
}
