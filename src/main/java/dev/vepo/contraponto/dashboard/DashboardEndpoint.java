package dev.vepo.contraponto.dashboard;

import java.util.List;
import java.util.Map;

import dev.vepo.contraponto.custompage.CustomPageRepository;
import dev.vepo.contraponto.custompage.Links;
import dev.vepo.contraponto.navigation.BreadcrumbService;
import dev.vepo.contraponto.navigation.BreadcrumbTrail;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.view.ViewRepository;
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
@Path("/dashboard")
@ApplicationScoped
public class DashboardEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance analytics(DashboardAnalytics analytics);

        public static native TemplateInstance dashboard(DashboardPage page,
                                                        Links links,
                                                        LoggedUser user,
                                                        BreadcrumbTrail breadcrumb);

        public static native TemplateInstance panel(DashboardPage page);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final PostRepository postRepository;
    private final ViewRepository viewRepository;
    private final CustomPageRepository customPageRepository;
    private final DashboardAnalyticsService analyticsService;
    private final LoggedUser loggedUser;
    private final BreadcrumbService breadcrumbService;

    @Inject
    public DashboardEndpoint(PostRepository postRepository,
                             ViewRepository viewRepository,
                             CustomPageRepository customPageRepository,
                             DashboardAnalyticsService analyticsService,
                             LoggedUser loggedUser,
                             BreadcrumbService breadcrumbService) {
        this.postRepository = postRepository;
        this.viewRepository = viewRepository;
        this.customPageRepository = customPageRepository;
        this.analyticsService = analyticsService;
        this.loggedUser = loggedUser;
        this.breadcrumbService = breadcrumbService;
    }

    @GET
    @Path("components/analytics")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance analytics(@QueryParam("blogId") Long blogId,
                                      @QueryParam("year") Integer year,
                                      @QueryParam("month") Integer month,
                                      @QueryParam("compare") @DefaultValue("false") boolean compare) {
        return Templates.analytics(analyticsService.load(blogId, year, month, compare));
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response dashboard(@QueryParam("blogId") Long blogId) {
        var builder = UriBuilder.fromPath("/manage/dashboard");
        if (blogId != null) {
            builder.queryParam("blogId", blogId);
        }
        return Response.seeOther(builder.build()).build();
    }
}
