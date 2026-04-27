package dev.vepo.contraponto.components;

import org.eclipse.microprofile.openapi.annotations.Operation;

import dev.vepo.contraponto.shared.infra.LoggedUser;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@ApplicationScoped
@Path("components/write-btn")
public class WriteButtonEndpoint {
    @CheckedTemplate
    @SuppressWarnings("java:S1118")
    public static class Templates {
        public static native TemplateInstance writeBtn(LoggedUser user);
    }

    private final LoggedUser loggedUser;

    @Inject
    public WriteButtonEndpoint(LoggedUser loggedUser) {
        this.loggedUser = loggedUser;
    }

    @GET
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance menu() {
        return Templates.writeBtn(loggedUser);
    }
}
