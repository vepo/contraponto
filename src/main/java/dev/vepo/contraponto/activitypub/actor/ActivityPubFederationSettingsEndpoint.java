package dev.vepo.contraponto.activitypub.actor;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.user.LoggedUser;
import dev.vepo.contraponto.user.UserRepository;

@Logged
@ApplicationScoped
@Path("/forms/writing/activitypub")
public class ActivityPubFederationSettingsEndpoint {

    private final LoggedUser loggedUser;
    private final UserRepository userRepository;
    private final ActivityPubActorService actorService;

    @Inject
    public ActivityPubFederationSettingsEndpoint(LoggedUser loggedUser,
                                                 UserRepository userRepository,
                                                 ActivityPubActorService actorService) {
        this.loggedUser = loggedUser;
        this.userRepository = userRepository;
        this.actorService = actorService;
    }

    @POST
    @Transactional
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response toggle(@FormParam("federationEnabled") String federationEnabled) {
        if (!loggedUser.isAuthenticated()) {
            return Response.status(Status.FORBIDDEN).build();
        }
        var user = userRepository.findById(loggedUser.getId()).orElseThrow(() -> new jakarta.ws.rs.NotFoundException());
        if ("true".equalsIgnoreCase(federationEnabled) || "on".equalsIgnoreCase(federationEnabled)) {
            actorService.enableFederation(user);
            return Response.ok("<div class=\"success-message\">Fediverse publishing enabled.</div>").build();
        }
        if (actorService.findByUserId(user.getId()).isPresent()) {
            actorService.disableFederation(user);
        }
        return Response.ok("<div class=\"success-message\">Fediverse publishing disabled.</div>").build();
    }
}
