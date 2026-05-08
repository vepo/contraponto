package dev.vepo.contraponto.search;

import java.util.Objects;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
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

        public static native TemplateInstance search(String query, Page<Post> results, LoggedUser user);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final PostRepository postRepository;
    private final LoggedUser loggedUser;

    @Inject
    public SearchEndpoint(PostRepository postRepository, LoggedUser loggedUser) {
        this.postRepository = postRepository;
        this.loggedUser = loggedUser;
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
        if (Objects.nonNull(query) && !query.isBlank()) {
            return Templates.search(query, postRepository.search(query, PageQuery.forGrid(20, 1)), loggedUser);
        } else {
            return Templates.search(query, null, loggedUser);
        }
    }
}