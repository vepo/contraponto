package dev.vepo.contraponto.components;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.user.LoggedUser;
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
    public static class Templates {
        public static native TemplateInstance menu(LoggedUser user, MenuNavigation nav);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(MenuEndpoint.class);

    private final LoggedUser loggedUser;

    private final MenuNavigationService menuNavigationService;

    @Inject
    public MenuEndpoint(LoggedUser loggedUser, MenuNavigationService menuNavigationService) {
        this.loggedUser = loggedUser;
        this.menuNavigationService = menuNavigationService;
    }

    @GET
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance menu() {
        logger.info("Reloading meny...");
        return Templates.menu(loggedUser, menuNavigationService.build(loggedUser));
    }
}
