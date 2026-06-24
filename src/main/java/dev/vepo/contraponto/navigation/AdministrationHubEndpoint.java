package dev.vepo.contraponto.navigation;

import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.user.LoggedUser;
import dev.vepo.contraponto.shared.i18n.I18nDefaults;
import dev.vepo.contraponto.shared.i18n.I18nKeys;
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
@Path("/administration")
@ApplicationScoped
public class AdministrationHubEndpoint {

    private final NavigationHubService hubService;
    private final LoggedUser loggedUser;

    @Inject
    public AdministrationHubEndpoint(NavigationHubService hubService, LoggedUser loggedUser) {
        this.hubService = hubService;
        this.loggedUser = loggedUser;
    }

    private Response forbidden() {
        return Toast.response(Response.Status.FORBIDDEN)
                    .i18nKey(I18nKeys.TOAST_ADMIN_FORBIDDEN, I18nDefaults.ADMIN_FORBIDDEN)
                    .type(Toast.Type.ERROR)
                    .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                    .build();
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response hub() {
        if (!loggedUser.isUserAdministrator()) {
            return forbidden();
        }
        return Response.ok(hubService.shell(NavigationHub.ADMINISTRATION,
                                            hubService.defaultSectionSlug(NavigationHub.ADMINISTRATION),
                                            1))
                       .build();
    }

    @GET
    @Path("{section}")
    @Produces(MediaType.TEXT_HTML)
    public Response section(@PathParam("section") String section,
                            @QueryParam("page") @DefaultValue("1") int page) {
        if (!loggedUser.isUserAdministrator()) {
            return forbidden();
        }
        return Response.ok(hubService.shell(NavigationHub.ADMINISTRATION, section, page)).build();
    }
}
