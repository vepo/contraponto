package dev.vepo.contraponto.custompage;

import org.eclipse.microprofile.openapi.annotations.Operation;

import dev.vepo.contraponto.shared.infra.LoggedUser;
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

@Path("/_custom_page")
@ApplicationScoped
public class CustomPageEndpoint {

    @CheckedTemplate
    public static class Templates {
        static native TemplateInstance page(CustomPage page, Links links, LoggedUser user);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final CustomPageRepository customPageRepository;
    private final LoggedUser loggedUser;

    @Inject
    public CustomPageEndpoint(CustomPageRepository customPageRepository, LoggedUser loggedUser) {
        this.customPageRepository = customPageRepository;
        this.loggedUser = loggedUser;
    }

    @GET
    @Path("blog/{username}/{blogSlug}/{slug}")
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance blogPage(@PathParam("username") String username,
                                     @PathParam("blogSlug") String blogSlug,
                                     @PathParam("slug") String slug) {
        var page = customPageRepository.findByUsernameBlogSlugAndSlug(username, blogSlug, slug)
                                       .orElseThrow(NotFoundException::new);
        return Templates.page(page, customPageRepository.loadLinks(CustomPagePaths.linksBlogId(page)), loggedUser);
    }

    @GET
    @Path("global/{slug}")
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance globalPage(@PathParam("slug") String slug) {
        var page = customPageRepository.findGlobalBySlug(slug).orElseThrow(NotFoundException::new);
        return Templates.page(page, customPageRepository.loadLinks(), loggedUser);
    }

    @GET
    @Path("user/{username}/{slug}")
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance userPage(@PathParam("username") String username, @PathParam("slug") String slug) {
        var page = customPageRepository.findByUsernameAndSlug(username, slug).orElseThrow(NotFoundException::new);
        return Templates.page(page, customPageRepository.loadLinks(CustomPagePaths.linksBlogId(page)), loggedUser);
    }
}
