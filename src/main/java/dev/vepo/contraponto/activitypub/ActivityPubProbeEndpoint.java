package dev.vepo.contraponto.activitypub;

import org.eclipse.microprofile.openapi.annotations.Operation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path(ActivityPubIngressPaths.INTERNAL_PREFIX + "/user/{username}/poco")
@ApplicationScoped
public class ActivityPubProbeEndpoint {

    @GET
    @Operation(hidden = true)
    public void poco(@PathParam("username") String username) {
        throw new NotFoundException();
    }
}
