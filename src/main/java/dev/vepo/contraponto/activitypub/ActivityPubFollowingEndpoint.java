package dev.vepo.contraponto.activitypub;

import org.eclipse.microprofile.openapi.annotations.Operation;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.vepo.contraponto.user.UserRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

@Path(ActivityPubIngressPaths.INTERNAL_PREFIX + "/user/{username}/following")
@ApplicationScoped
public class ActivityPubFollowingEndpoint {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final ActivityPubActorService actorService;
    private final ActivityPubOutboxService outboxService;
    private final UserRepository userRepository;

    @Inject
    public ActivityPubFollowingEndpoint(ActivityPubActorService actorService,
                                        ActivityPubOutboxService outboxService,
                                        UserRepository userRepository) {
        this.actorService = actorService;
        this.outboxService = outboxService;
        this.userRepository = userRepository;
    }

    @GET
    @Operation(hidden = true)
    @Produces({ ActivityPubPaths.ACTIVITY_JSON, ActivityPubPaths.LD_JSON })
    public Response following(@PathParam("username") String username) throws Exception {
        actorService.findEnabledByUsername(username).orElseThrow(NotFoundException::new);
        var user = userRepository.findByUsername(username).orElseThrow(NotFoundException::new);
        var document = outboxService.buildFollowingCollection(user);
        return Response.ok(JSON.writeValueAsString(document)).build();
    }
}
