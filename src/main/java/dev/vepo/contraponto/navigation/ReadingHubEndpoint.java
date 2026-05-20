package dev.vepo.contraponto.navigation;

import dev.vepo.contraponto.shared.infra.Logged;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Logged
@Path("/reading")
@ApplicationScoped
public class ReadingHubEndpoint {

    private final NavigationHubService hubService;

    @Inject
    public ReadingHubEndpoint(NavigationHubService hubService) {
        this.hubService = hubService;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance hub() {
        return hubService.shell(NavigationHub.READING, hubService.defaultSectionSlug(NavigationHub.READING), 1);
    }

    @GET
    @Path("{section}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance section(@PathParam("section") String section,
                                    @QueryParam("page") @DefaultValue("1") int page) {
        return hubService.shell(NavigationHub.READING, section, page);
    }
}
