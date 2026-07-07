package dev.vepo.contraponto.activitypub;

import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.user.LoggedUser;
import dev.vepo.contraponto.user.Role;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Logged
@ApplicationScoped
@Path("/forms/administration/activitypub")
public class ActivityPubPlatformSettingsEndpoint {

    private final LoggedUser loggedUser;
    private final ActivityPubSettings settings;

    @Inject
    public ActivityPubPlatformSettingsEndpoint(LoggedUser loggedUser, ActivityPubSettings settings) {
        this.loggedUser = loggedUser;
        this.settings = settings;
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response toggle(@FormParam("platformFederationEnabled") String enabledParam) {
        if (!loggedUser.hasRole(Role.ADMIN)) {
            return Response.status(Status.FORBIDDEN).build();
        }
        if (!settings.configEnabled()) {
            return Response.ok("<div class=\"error-message visible\">ActivityPub is disabled by configuration.</div>").build();
        }
        boolean enabled = "true".equalsIgnoreCase(enabledParam) || "on".equalsIgnoreCase(enabledParam);
        settings.setPlatformFederationEnabled(enabled);
        var message = enabled ? "ActivityPub federation globally enabled." : "ActivityPub federation globally disabled.";
        return Response.ok("<div class=\"success-message\">%s</div>".formatted(message)).build();
    }
}
