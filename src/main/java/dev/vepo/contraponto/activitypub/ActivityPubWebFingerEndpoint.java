package dev.vepo.contraponto.activitypub;

import org.eclipse.microprofile.openapi.annotations.Operation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

@Path(ActivityPubIngressPaths.INTERNAL_PREFIX + "/well-known/webfinger")
@ApplicationScoped
public class ActivityPubWebFingerEndpoint {

    private final ActivityPubWebFingerService webFingerService;

    @Inject
    public ActivityPubWebFingerEndpoint(ActivityPubWebFingerService webFingerService) {
        this.webFingerService = webFingerService;
    }

    @GET
    @Operation(hidden = true)
    @Produces(ActivityPubWebFingerJrdProvider.JRD_JSON)
    public WebFingerJrd webfinger(@QueryParam("resource") String resource) {
        return webFingerService.resolve(resource)
                               .orElseThrow(NotFoundException::new);
    }
}
