package dev.vepo.contraponto.activitypub;

import org.eclipse.microprofile.openapi.annotations.Operation;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

@Path("/.well-known/webfinger")
@ApplicationScoped
public class ActivityPubWebFingerEndpoint {

    private static final String JRD_JSON = "application/jrd+json";

    private static final ObjectMapper JSON = new ObjectMapper();

    private final ActivityPubWebFingerService webFingerService;

    @Inject
    public ActivityPubWebFingerEndpoint(ActivityPubWebFingerService webFingerService) {
        this.webFingerService = webFingerService;
    }

    @GET
    @Operation(hidden = true)
    @Produces(JRD_JSON)
    public Response webfinger(@jakarta.ws.rs.QueryParam("resource") String resource) throws Exception {
        var document = webFingerService.resolve(resource).orElseThrow(NotFoundException::new);
        return Response.ok(JSON.writeValueAsString(document)).type(JRD_JSON).build();
    }
}
