package dev.vepo.contraponto.activitypub.discovery;

import org.eclipse.microprofile.openapi.annotations.Operation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path(ActivityPubIngressPaths.INTERNAL_PREFIX + "/well-known/nodeinfo")
@ApplicationScoped
public class ActivityPubNodeInfoWellKnownEndpoint {

    private final ActivityPubNodeInfoService nodeInfoService;

    @Inject
    public ActivityPubNodeInfoWellKnownEndpoint(ActivityPubNodeInfoService nodeInfoService) {
        this.nodeInfoService = nodeInfoService;
    }

    @GET
    @Operation(hidden = true)
    @Produces(ActivityPubWebFingerJrdProvider.JRD_JSON)
    public WebFingerJrd wellKnownNodeInfo() {
        return nodeInfoService.buildWellKnownDocument();
    }
}
