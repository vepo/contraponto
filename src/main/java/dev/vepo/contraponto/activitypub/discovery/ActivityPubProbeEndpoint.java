package dev.vepo.contraponto.activitypub.discovery;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;

@Path(ActivityPubIngressPaths.INTERNAL_PREFIX + "/user/{username}/poco")
@ApplicationScoped
public class ActivityPubProbeEndpoint {

    @GET
    @Operation(hidden = true)
    public Response poco(@PathParam("username") String username) {
        return Response.status(Response.Status.NOT_FOUND).build();
    }
}
