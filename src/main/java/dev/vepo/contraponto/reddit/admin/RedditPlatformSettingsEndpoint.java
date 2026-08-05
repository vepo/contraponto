package dev.vepo.contraponto.reddit.admin;

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

import dev.vepo.contraponto.reddit.RedditSettings;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.user.LoggedUser;
import dev.vepo.contraponto.user.Role;

@Logged
@ApplicationScoped
@Path("/forms/administration/reddit")
public class RedditPlatformSettingsEndpoint {

    private final LoggedUser loggedUser;
    private final RedditSettings settings;

    @Inject
    public RedditPlatformSettingsEndpoint(LoggedUser loggedUser, RedditSettings settings) {
        this.loggedUser = loggedUser;
        this.settings = settings;
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response toggle(@FormParam("platformSyndicationEnabled") String enabledParam) {
        if (!loggedUser.hasRole(Role.ADMIN)) {
            return Response.status(Status.FORBIDDEN).build();
        }
        if (!settings.configEnabled()) {
            return Response.ok("<div class=\"error-message visible\">Reddit is disabled by configuration.</div>").build();
        }
        boolean enabled = "true".equalsIgnoreCase(enabledParam) || "on".equalsIgnoreCase(enabledParam);
        settings.setPlatformSyndicationEnabled(enabled);
        var message = enabled ? "Reddit syndication globally enabled." : "Reddit syndication globally disabled.";
        return Response.ok("<div class=\"success-message\">%s</div>".formatted(message)).build();
    }
}
