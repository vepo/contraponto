package dev.vepo.contraponto.activitypub;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.Operation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path(ActivityPubIngressPaths.INTERNAL_PREFIX + "/user/{username}/inbox")
@ApplicationScoped
public class ActivityPubInboxEndpoint {

    private final ActivityPubInboxService inboxService;

    @Inject
    public ActivityPubInboxEndpoint(ActivityPubInboxService inboxService) {
        this.inboxService = inboxService;
    }

    @POST
    @Operation(hidden = true)
    @Consumes({ ActivityPubPaths.ACTIVITY_JSON, ActivityPubPaths.LD_JSON })
    @Produces({ ActivityPubPaths.ACTIVITY_JSON, ActivityPubPaths.LD_JSON })
    public Response inbox(@PathParam("username") String username,
                          String body,
                          @Context UriInfo uriInfo,
                          @Context HttpHeaders httpHeaders) {
        var headers = toHeaderMap(httpHeaders);
        var requestUri = uriInfo.getRequestUri();
        inboxService.handleInbox(username, body, headers, URI.create(requestUri.toString()));
        return Response.accepted().build();
    }

    private Map<String, String> toHeaderMap(HttpHeaders httpHeaders) {
        var map = new LinkedHashMap<String, String>();
        httpHeaders.getRequestHeaders().forEach((name, values) -> {
            if (!values.isEmpty()) {
                map.put(name, values.get(0));
            }
        });
        return map;
    }
}
