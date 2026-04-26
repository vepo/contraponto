package dev.vepo.contraponto.components;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.shared.infra.LoggedUser;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("components/menu")
@ApplicationScoped
public class MenuEndpoint {
    @CheckedTemplate
    @SuppressWarnings("java:S1118")
    public static class Templates {
        public static native TemplateInstance menu(LoggedUser user);
    }

    private static final Logger logger = LoggerFactory.getLogger(MenuEndpoint.class);

    private final LoggedUser loggedUser;

    @Inject
    public MenuEndpoint(LoggedUser loggedUser) {
        this.loggedUser = loggedUser;
    }

    @GET
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance menu() {
        logger.info("Reloading meny...");
        return Templates.menu(loggedUser);
    }
}
