package dev.vepo.contraponto.components;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/auth/modal")
@ApplicationScoped
public class AuthModal {
    @CheckedTemplate
    public static class Templates {
        static native TemplateInstance modal(String mode);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance modal(@QueryParam("mode") String mode) {
        return Templates.modal(mode); // mode = "login" or "signup"
    }
}
