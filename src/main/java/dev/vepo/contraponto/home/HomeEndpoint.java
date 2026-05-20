package dev.vepo.contraponto.home;

import org.eclipse.microprofile.openapi.annotations.Operation;

import dev.vepo.contraponto.custompage.CustomPageRepository;
import dev.vepo.contraponto.custompage.Links;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.seo.SeoMetadata;
import dev.vepo.contraponto.seo.SeoService;
import dev.vepo.contraponto.tag.AuthorTagUsage;
import dev.vepo.contraponto.tag.TagUsage;
import java.util.Collections;
import java.util.List;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/")
@ApplicationScoped
public class HomeEndpoint {

    @CheckedTemplate
    public static class Templates {
        static native TemplateInstance featured(Post post);

        static native TemplateInstance grid(Page<Post> posts, boolean ignoreFirst);

        static native TemplateInstance home(Page<Post> posts,
                                            Links links,
                                            LoggedUser user,
                                            SeoMetadata seo,
                                            List<TagUsage> topTags,
                                            List<AuthorTagUsage> mainAuthors,
                                            long totalAuthors);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final PostRepository postRepository;
    private final CustomPageRepository customPageRepository;
    private final LoggedUser loggedUser;
    private final SeoService seoService;

    @Inject
    public HomeEndpoint(PostRepository postRepository,
                        CustomPageRepository customPageRepository,
                        LoggedUser loggedUser,
                        SeoService seoService) {
        this.postRepository = postRepository;
        this.customPageRepository = customPageRepository;
        this.loggedUser = loggedUser;
        this.seoService = seoService;
    }

    @GET
    @Path("components/home/grid")
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance grid(@QueryParam("limit") @DefaultValue("12") int limit, @QueryParam("page") int page) {
        return Templates.grid(this.postRepository.findFeatured(PageQuery.forFeaturedGrid(limit, page)), false);
    }

    @GET
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance home(@QueryParam("limit") @DefaultValue("12") int limit) {
        return Templates.home(this.postRepository.findFeatured(PageQuery.forFeaturedGrid(limit, 1)),
                              customPageRepository.loadLinks(),
                              loggedUser,
                              seoService.forHome(),
                              Collections.emptyList(),
                              Collections.emptyList(),
                              0L);
    }
}