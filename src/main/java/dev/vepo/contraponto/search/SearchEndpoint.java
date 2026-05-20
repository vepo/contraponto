package dev.vepo.contraponto.search;

import java.util.Objects;

import dev.vepo.contraponto.custompage.CustomPageRepository;
import dev.vepo.contraponto.custompage.Links;
import dev.vepo.contraponto.navigation.BreadcrumbService;
import dev.vepo.contraponto.navigation.BreadcrumbTrail;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.seo.SeoService;
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

@Path("/search")
@ApplicationScoped
public class SearchEndpoint {

    @CheckedTemplate
    public static class Templates {

        public static native TemplateInstance modal();

        public static native TemplateInstance results(LoggedUser user, Page<Post> results, String query);

        public static native TemplateInstance search(String query,
                                                     Page<Post> results,
                                                     Links links,
                                                     LoggedUser user,
                                                     BreadcrumbTrail breadcrumb,
                                                     dev.vepo.contraponto.seo.SeoMetadata seo);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final PostRepository postRepository;
    private final CustomPageRepository customPageRepository;
    private final LoggedUser loggedUser;
    private final BreadcrumbService breadcrumbService;
    private final SeoService seoService;

    @Inject
    public SearchEndpoint(PostRepository postRepository,
                          CustomPageRepository customPageRepository,
                          LoggedUser loggedUser,
                          BreadcrumbService breadcrumbService,
                          SeoService seoService) {
        this.postRepository = postRepository;
        this.customPageRepository = customPageRepository;
        this.loggedUser = loggedUser;
        this.breadcrumbService = breadcrumbService;
        this.seoService = seoService;
    }

    // Fragment endpoint for HTMX infinite scroll / load more
    @GET
    @Path("/results")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance resultsFragment(@QueryParam("q") String query, @DefaultValue("1") @QueryParam("page") int page) {
        if (Objects.nonNull(query) && !query.isBlank()) {
            Page<Post> results = postRepository.search(query, PageQuery.forGrid(20, page));
            return Templates.results(loggedUser, results, query);
        } else {
            return Templates.results(loggedUser, null, query);
        }
    }

    // Modal for quick search
    @GET
    @Path("/modal")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance searchModal() {
        return Templates.modal();
    }

    // Main search page
    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance searchPage(@QueryParam("q") String query) {
        var breadcrumb = breadcrumbService.forSearch();
        if (Objects.nonNull(query) && !query.isBlank()) {
            return Templates.search(query,
                                    postRepository.search(query, PageQuery.forGrid(20, 1)),
                                    customPageRepository.loadLinks(),
                                    loggedUser,
                                    breadcrumb,
                                    seoService.forSearch(query));
        } else {
            return Templates.search(query,
                                    null,
                                    customPageRepository.loadLinks(),
                                    loggedUser,
                                    breadcrumb,
                                    seoService.forSearch(query));
        }
    }
}