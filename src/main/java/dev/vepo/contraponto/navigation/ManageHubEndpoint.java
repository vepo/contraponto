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
@Path("/manage")
@ApplicationScoped
public class ManageHubEndpoint {

    private final NavigationHubService hubService;

    @Inject
    public ManageHubEndpoint(NavigationHubService hubService) {
        this.hubService = hubService;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance hub() {
        return hubService.shell(NavigationHub.MANAGE, hubService.defaultSectionSlug(NavigationHub.MANAGE), 1);
    }

    @GET
    @Path("{section}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance section(@PathParam("section") String section,
                                    @QueryParam("page") @DefaultValue("1") int page) {
        return hubService.shell(NavigationHub.MANAGE, section, page);
    }
}
