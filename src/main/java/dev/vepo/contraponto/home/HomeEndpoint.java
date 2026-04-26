package dev.vepo.contraponto.home;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.shared.pagination.Page;
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

        static native TemplateInstance home(Page<Post> posts, LoggedUser user);
    }

    private final PostRepository postRepository;
    private final LoggedUser loggedUser;

    @Inject
    public HomeEndpoint(PostRepository postRepository, LoggedUser loggedUser) {
        this.postRepository = postRepository;
        this.loggedUser = loggedUser;
    }

    @GET
    @Path("components/home/grid")
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance morePosts(@QueryParam("limit") @DefaultValue("12") int limit, @QueryParam("page") int page) {
        return Templates.grid(this.postRepository.findPaginatedNewest(limit, page), false);
    }

    @GET
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance post(@QueryParam("limit") @DefaultValue("12") int limit) {
        return Templates.home(this.postRepository.findPaginatedNewest(limit, 1), loggedUser);
    }
}