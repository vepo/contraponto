package dev.vepo.contraponto.components;

import dev.vepo.contraponto.shared.infra.AvatarSvgRenderer;
import dev.vepo.contraponto.shared.infra.DisplayNameInitials;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Response;

@Path("/components/avatar")
@ApplicationScoped
public class AvatarEndpoint {

    private static final int MAX_NAME_LENGTH = 255;

    @GET
    @Produces("image/svg+xml")
    public Response avatar(@QueryParam("name") String name) {
        if (name == null || name.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if (name.length() > MAX_NAME_LENGTH) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        var svg = AvatarSvgRenderer.render(DisplayNameInitials.from(name));
        var cacheControl = new CacheControl();
        cacheControl.setMaxAge(86_400);

        return Response.ok(svg)
                       .type("image/svg+xml")
                       .cacheControl(cacheControl)
                       .build();
    }
}
