package dev.vepo.contraponto.search;

import java.time.LocalDateTime;
import java.util.List;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.shared.infra.LoggedUser;
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
        public static native TemplateInstance search(String query, List<Post> results, long total, int currentYear, LoggedUser user);

        public static native TemplateInstance results(LoggedUser user, List<Post> results, int page, long total, String query, int currentYear);

        public static native TemplateInstance modal();
    }

    private final PostRepository postRepository;
    private final LoggedUser loggedUser;

    @Inject
    public SearchEndpoint(PostRepository postRepository, LoggedUser loggedUser) {
        this.postRepository = postRepository;
        this.loggedUser = loggedUser;
    }

    // Main search page
    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance searchPage(@QueryParam("q") String query) {
        List<Post> results = List.of();
        long total = 0;
        if (query != null && !query.isBlank()) {
            results = postRepository.search(query, 20, 0);
            total = postRepository.countSearchResults(query);
        }
        return Templates.search(query, results, total, LocalDateTime.now().getYear(), loggedUser);
    }

    // Fragment endpoint for HTMX infinite scroll / load more
    @GET
    @Path("/results")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance resultsFragment(@QueryParam("q") String query, @DefaultValue("1") @QueryParam("page") int page) {
        int limit = 10;
        int offset = (page - 1) * limit;
        List<Post> results = postRepository.search(query, limit, offset);
        long total = postRepository.countSearchResults(query);
        return Templates.results(loggedUser, results, page, total, query, LocalDateTime.now().getYear());
    }

    // Modal for quick search
    @GET
    @Path("/modal")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance searchModal() {
        return Templates.modal();
    }
}