package dev.vepo.contraponto.activitypub;

import org.eclipse.microprofile.openapi.annotations.Operation;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

@Path(ActivityPubIngressPaths.INTERNAL_PREFIX + "/well-known/nodeinfo")
@ApplicationScoped
public class ActivityPubNodeInfoWellKnownEndpoint {

    private static final ObjectMapper JSON = new ObjectMapper();

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
