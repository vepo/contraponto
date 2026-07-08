package dev.vepo.contraponto.activitypub.discovery;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.microprofile.openapi.annotations.Operation;

@Path(ActivityPubIngressPaths.INTERNAL_PREFIX + "/nodeinfo")
@ApplicationScoped
public class ActivityPubNodeInfoEndpoint {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final ActivityPubNodeInfoService nodeInfoService;

    @Inject
    public ActivityPubNodeInfoEndpoint(ActivityPubNodeInfoService nodeInfoService) {
        this.nodeInfoService = nodeInfoService;
    }

    @GET
    @Path("2.0")
    @Operation(hidden = true)
    @Produces("application/json")
    public Response nodeInfo20() throws Exception {
        return Response.ok(JSON.writeValueAsString(nodeInfoService.buildNodeInfo20Document())).build();
    }

    @GET
    @Path("2.1")
    @Operation(hidden = true)
    @Produces("application/json")
    public Response nodeInfo21() throws Exception {
        return Response.ok(JSON.writeValueAsString(nodeInfoService.buildNodeInfo20Document())).build();
    }
}
