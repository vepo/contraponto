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
@Path("/account")
@ApplicationScoped
public class AccountHubEndpoint {

    private final NavigationHubService hubService;

    @Inject
    public AccountHubEndpoint(NavigationHubService hubService) {
        this.hubService = hubService;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance hub() {
        return hubService.shell(NavigationHub.ACCOUNT, hubService.defaultSectionSlug(NavigationHub.ACCOUNT), 1);
    }

    @GET
    @Path("{section}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance section(@PathParam("section") String section,
                                    @QueryParam("page") @DefaultValue("1") int page,
                                    @QueryParam("verified") @DefaultValue("false") boolean emailVerified,
                                    @QueryParam("error") String error) {
        return hubService.shell(NavigationHub.ACCOUNT, section, page, emailVerified, error);
    }
}
