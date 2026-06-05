package dev.vepo.contraponto.serie;

import org.eclipse.microprofile.openapi.annotations.Operation;

import java.util.List;

import dev.vepo.contraponto.custompage.CustomPageRepository;
import dev.vepo.contraponto.custompage.Links;
import dev.vepo.contraponto.navigation.BreadcrumbService;
import dev.vepo.contraponto.navigation.BreadcrumbTrail;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
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
import jakarta.ws.rs.core.Response;

@Path("{username}")
@ApplicationScoped
public class SeriePageEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance serie(Serie serie,
                                                    List<Post> posts,
                                                    Links links,
                                                    LoggedUser user,
                                                    BreadcrumbTrail breadcrumb,
                                                    dev.vepo.contraponto.seo.SeoMetadata seo);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    public static String extractUrl(Serie serie) {
        var blog = serie.getBlog();
        if (blog.isMain()) {
            return "/%s/serie/%s".formatted(blog.getOwner().getUsername(), serie.getSlug());
        }
        return "/%s/%s/serie/%s".formatted(blog.getOwner().getUsername(), blog.getSlug(), serie.getSlug());
    }

    private final SerieRepository serieRepository;
    private final PostRepository postRepository;
    private final CustomPageRepository customPageRepository;
    private final LoggedUser loggedUser;
    private final BreadcrumbService breadcrumbService;
    private final SeoService seoService;

    @Inject
    public SeriePageEndpoint(SerieRepository serieRepository,
                             PostRepository postRepository,
                             CustomPageRepository customPageRepository,
                             LoggedUser loggedUser,
                             BreadcrumbService breadcrumbService,
                             SeoService seoService) {
        this.serieRepository = serieRepository;
        this.postRepository = postRepository;
        this.customPageRepository = customPageRepository;
        this.loggedUser = loggedUser;
        this.breadcrumbService = breadcrumbService;
        this.seoService = seoService;
    }

    @GET
    @Path("serie/{serieSlug}")
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public Response mainBlogSerie(@PathParam("username") String username, @PathParam("serieSlug") String serieSlug) {
        Serie serie = serieRepository.findMainBlogSerie(username, serieSlug)
                                     .orElseThrow(() -> new NotFoundException("Series not found"));
        return render(serie);
    }

    private Response render(Serie serie) {
        List<Post> posts = postRepository.findPublishedBySerieOrdered(serie.getId());
        Links links = serie.getBlog().isMain() ? customPageRepository.loadLinks()
                                               : customPageRepository.loadLinks(serie.getBlog().getId());
        var breadcrumb = breadcrumbService.forSerie(serie);
        return Response.ok(Templates.serie(serie,
                                           posts,
                                           links,
                                           loggedUser,
                                           breadcrumb,
                                           seoService.forSerie(serie, breadcrumb)))
                       .build();
    }

    @GET
    @Path("{blogSlug}/serie/{serieSlug}")
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public Response secondaryBlogSerie(@PathParam("username") String username,
                                       @PathParam("blogSlug") String blogSlug,
                                       @PathParam("serieSlug") String serieSlug) {
        Serie serie = serieRepository.findSecondaryBlogSerie(username, blogSlug, serieSlug)
                                     .orElseThrow(() -> new NotFoundException("Series not found"));
        return render(serie);
    }
}
