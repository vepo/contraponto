package dev.vepo.contraponto.dashboard;

import dev.vepo.contraponto.shared.infra.Logged;
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
@Path("/manage/dashboard/components")
@ApplicationScoped
public class DashboardEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance analytics(DashboardAnalytics analytics);

        public static native TemplateInstance panel(DashboardPage page);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final DashboardAnalyticsService analyticsService;

    @Inject
    public DashboardEndpoint(DashboardAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GET
    @Path("analytics")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance analytics(@QueryParam("blogId") Long blogId,
                                      @QueryParam("year") Integer year,
                                      @QueryParam("month") Integer month,
                                      @QueryParam("compare") @DefaultValue("false") boolean compare) {
        return Templates.analytics(analyticsService.load(blogId, year, month, compare));
    }

}
