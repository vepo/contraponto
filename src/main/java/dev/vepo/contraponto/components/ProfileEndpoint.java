package dev.vepo.contraponto.components;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.custompage.CustomPageRepository;
import dev.vepo.contraponto.custompage.Links;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Logged
@Path("/profile")
@ApplicationScoped
public class ProfileEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance profile(Links links, LoggedUser user);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(ProfileEndpoint.class);

    private final LoggedUser loggedUser;
    private final CustomPageRepository customPageRepository;

    @Inject
    public ProfileEndpoint(CustomPageRepository customPageRepository, LoggedUser loggedUser) {
        this.customPageRepository = customPageRepository;
        this.loggedUser = loggedUser;
    }

    @GET
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance profile() {
        logger.info("Reloading meny...");
        return Templates.profile(customPageRepository.loadLinks(), loggedUser);
    }
}