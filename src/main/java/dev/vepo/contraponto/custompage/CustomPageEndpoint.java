package dev.vepo.contraponto.custompage;

import dev.vepo.contraponto.user.UserRepository;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/page")
@ApplicationScoped
public class CustomPageEndpoint {
    @CheckedTemplate
    public static class Templates {
        static native TemplateInstance page(CustomPage page);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }

    }

    private final CustomPageRepository customPageRepository;

    @Inject
    public CustomPageEndpoint(UserRepository userRepository, CustomPageRepository customPageRepository) {
        this.customPageRepository = customPageRepository;
    }

    @GET
    @Path("/{slug}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance globalPage(@PathParam("slug") String slug) {
        return customPageRepository.findBySlug(slug)
                                   .map(Templates::page)
                                   .orElseThrow(() -> new NotFoundException("Page not found! %s".formatted(slug)));
    }

    @GET
    @Path("/{username}/{slug}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance userPage(@PathParam("username") String username,
                                     @PathParam("slug") String slug) {
        return customPageRepository.findByUsernameAndSlug(username, slug)
                                   .map(Templates::page)
                                   .orElseThrow(() -> new NotFoundException("Page not found! %s".formatted(slug)));
    }
}
