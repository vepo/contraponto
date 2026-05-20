package dev.vepo.contraponto.custompage;

import org.eclipse.microprofile.openapi.annotations.Operation;

import dev.vepo.contraponto.navigation.BreadcrumbService;
import dev.vepo.contraponto.navigation.BreadcrumbTrail;
import dev.vepo.contraponto.seo.SeoService;
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
        static native TemplateInstance page(CustomPage page,
                                            Links links,
                                            LoggedUser user,
                                            BreadcrumbTrail breadcrumb,
                                            dev.vepo.contraponto.seo.SeoMetadata seo);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final CustomPageCache customPageCache;
    private final CustomPageRepository customPageRepository;
    private final LoggedUser loggedUser;
    private final BreadcrumbService breadcrumbService;
    private final SeoService seoService;

    @Inject
    public CustomPageEndpoint(CustomPageCache customPageCache,
                              CustomPageRepository customPageRepository,
                              LoggedUser loggedUser,
                              BreadcrumbService breadcrumbService,
                              SeoService seoService) {
        this.customPageCache = customPageCache;
        this.customPageRepository = customPageRepository;
        this.loggedUser = loggedUser;
        this.breadcrumbService = breadcrumbService;
        this.seoService = seoService;
    }

    @GET
    @Path("blog/{username}/{blogSlug}/{slug}")
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance blogPage(@PathParam("username") String username,
                                     @PathParam("blogSlug") String blogSlug,
                                     @PathParam("slug") String slug) {
        var page = customPageCache.findByUsernameBlogSlugAndSlug(username, blogSlug, slug)
                                  .orElseThrow(NotFoundException::new);
        return Templates.page(page,
                              customPageRepository.loadLinks(CustomPagePaths.linksBlogId(page)),
                              loggedUser,
                              breadcrumbService.forCustomPage(page),
                              seoService.forCustomPage(page));
    }

    @GET
    @Path("global/{slug}")
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance globalPage(@PathParam("slug") String slug) {
        var page = customPageCache.findGlobalBySlug(slug).orElseThrow(NotFoundException::new);
        return Templates.page(page,
                              customPageRepository.loadLinks(),
                              loggedUser,
                              breadcrumbService.forCustomPage(page),
                              seoService.forCustomPage(page));
    }

    @GET
    @Path("user/{username}/{slug}")
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance userPage(@PathParam("username") String username, @PathParam("slug") String slug) {
        var page = customPageCache.findByUsernameAndSlug(username, slug).orElseThrow(NotFoundException::new);
        return Templates.page(page,
                              customPageRepository.loadLinks(CustomPagePaths.linksBlogId(page)),
                              loggedUser,
                              breadcrumbService.forCustomPage(page),
                              seoService.forCustomPage(page));
    }
}
