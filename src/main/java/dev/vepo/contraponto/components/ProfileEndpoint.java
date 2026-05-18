package dev.vepo.contraponto.components;

import org.eclipse.microprofile.openapi.annotations.Operation;

import dev.vepo.contraponto.shared.infra.Logged;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

@Logged
@Path("/profile")
@ApplicationScoped
public class ProfileEndpoint {

    @GET
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public Response profile(@QueryParam("verified") @DefaultValue("false") boolean emailVerified,
                            @QueryParam("error") String error) {
        var builder = UriBuilder.fromPath("/account/security");
        if (emailVerified) {
            builder.queryParam("verified", true);
        }
        if (error != null) {
            builder.queryParam("error", error);
        }
        return Response.seeOther(builder.build()).build();
    }
}
