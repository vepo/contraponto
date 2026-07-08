package dev.vepo.contraponto.activitypub;

import org.eclipse.microprofile.openapi.annotations.Operation;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

@Path(ActivityPubIngressPaths.INTERNAL_PREFIX + "/user/{username}/activities/{activityType}/{activityId}")
@ApplicationScoped
public class ActivityPubActivityEndpoint {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final ActivityPubActivityService activityService;

    @Inject
    public ActivityPubActivityEndpoint(ActivityPubActivityService activityService) {
        this.activityService = activityService;
    }

    @GET
    @Operation(hidden = true)
    @Produces({ ActivityPubPaths.ACTIVITY_JSON, ActivityPubPaths.LD_JSON })
    public Response activity(@PathParam("username") String username,
                             @PathParam("activityType") String activityType,
                             @PathParam("activityId") long activityId)
            throws Exception {
        var document = activityService.findActivity(username, activityType, activityId)
                                      .orElseThrow(NotFoundException::new);
        return Response.ok(JSON.writeValueAsString(document))
                       .type(ActivityPubPaths.ACTIVITY_JSON)
                       .build();
    }
}
