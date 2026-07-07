package dev.vepo.contraponto.activitypub;

import org.eclipse.microprofile.openapi.annotations.Operation;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

@Path("/.well-known/host-meta")
@ApplicationScoped
public class ActivityPubHostMetaEndpoint {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final ActivityPubWebFingerService webFingerService;

    @Inject
    public ActivityPubHostMetaEndpoint(ActivityPubWebFingerService webFingerService) {
        this.webFingerService = webFingerService;
    }

    @GET
    @Operation(hidden = true)
    @Produces("application/xrd+xml")
    public Response hostMeta() {
        var links = webFingerService.hostMetaLinks();
        var template = ((java.util.Map<?, ?>) ((java.util.List<?>) links.get("links")).get(0)).get("template").toString();
        var xml = """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <XRD xmlns="http://docs.oasis-open.org/ns/xri/xrd-1.0">
                    <Link rel="lrdd" type="application/xrd+xml" template="%s"/>
                  </XRD>
                  """.formatted(template);
        return Response.ok(xml).build();
    }
}
