package dev.vepo.contraponto.navigation;

import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.shared.toast.Toast;
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
import jakarta.ws.rs.core.Response;

@Logged
@Path("/editor")
@ApplicationScoped
public class EditorHubEndpoint {

    private final NavigationHubService hubService;
    private final LoggedUser loggedUser;

    @Inject
    public EditorHubEndpoint(NavigationHubService hubService, LoggedUser loggedUser) {
        this.hubService = hubService;
        this.loggedUser = loggedUser;
    }

    private Response forbidden() {
        return Toast.response(Response.Status.FORBIDDEN)
                    .message("Usuário não possui permissões de editor!")
                    .type(Toast.Type.ERROR)
                    .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                    .build();
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response hub() {
        if (!loggedUser.isEditor()) {
            return forbidden();
        }
        return Response.ok(hubService.shell(NavigationHub.REVIEW, hubService.defaultSectionSlug(NavigationHub.REVIEW), 1))
                       .build();
    }

    @GET
    @Path("{section}")
    @Produces(MediaType.TEXT_HTML)
    public Response section(@PathParam("section") String section,
                            @QueryParam("page") @DefaultValue("1") int page) {
        if (!loggedUser.isEditor()) {
            return forbidden();
        }
        return Response.ok(hubService.shell(NavigationHub.REVIEW, section, page)).build();
    }
}
