package dev.vepo.contraponto.platforminsights;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import dev.vepo.contraponto.shared.infra.Logged;

@Logged
@Path("/administration/insights/components")
@ApplicationScoped
public class PlatformInsightsEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance analytics(PlatformInsights insights);

        public static native TemplateInstance panel();

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final PlatformInsightsService insightsService;

    @Inject
    public PlatformInsightsEndpoint(PlatformInsightsService insightsService) {
        this.insightsService = insightsService;
    }

    @GET
    @Path("analytics")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance analytics(@QueryParam("year") Integer year, @QueryParam("month") Integer month) {
        return Templates.analytics(insightsService.load(year, month));
    }

    public TemplateInstance renderHubPanel() {
        return Templates.panel();
    }
}
