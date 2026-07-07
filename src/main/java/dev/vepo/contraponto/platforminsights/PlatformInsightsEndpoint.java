package dev.vepo.contraponto.platforminsights;

import dev.vepo.contraponto.activitypub.ActivityPubSettings;
import dev.vepo.contraponto.shared.infra.Logged;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Logged
@Path("/administration/insights/components")
@ApplicationScoped
public class PlatformInsightsEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance analytics(PlatformInsights insights);

        public static native TemplateInstance panel(boolean activityPubConfigEnabled, boolean activityPubPlatformEnabled);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final PlatformInsightsService insightsService;
    private final ActivityPubSettings activityPubSettings;

    @Inject
    public PlatformInsightsEndpoint(PlatformInsightsService insightsService, ActivityPubSettings activityPubSettings) {
        this.insightsService = insightsService;
        this.activityPubSettings = activityPubSettings;
    }

    @GET
    @Path("analytics")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance analytics(@QueryParam("year") Integer year, @QueryParam("month") Integer month) {
        return Templates.analytics(insightsService.load(year, month));
    }

    public TemplateInstance renderHubPanel() {
        return Templates.panel(activityPubSettings.configEnabled(), activityPubSettings.enabled());
    }
}
