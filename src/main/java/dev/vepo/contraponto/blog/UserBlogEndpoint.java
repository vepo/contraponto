package dev.vepo.contraponto.blog;

import org.eclipse.microprofile.openapi.annotations.Operation;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.user.UserRepository;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/{username}")
@ApplicationScoped
public class UserBlogEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance home(String username, Page<Post> posts, LoggedUser user);

        static native TemplateInstance featured(Post post);

        static native TemplateInstance grid(String username, Page<Post> posts, boolean ignoreFirst);
    }

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final LoggedUser loggedUser;

    @Inject
    public UserBlogEndpoint(UserRepository userRepository, PostRepository postRepository, LoggedUser loggedUser) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.loggedUser = loggedUser;
    }

    @GET
    @Path("components/home/grid")
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance morePosts(@PathParam("username") String username, @QueryParam("limit") @DefaultValue("12") int limit,
                                      @QueryParam("page") int page) {
        return Templates.grid(username, this.postRepository.findPaginatedNewestFromAuthor(username, limit, page), false);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance blog(@PathParam("username") String username, @QueryParam("limit") @DefaultValue("12") int limit) {
        // Check if user exists
        var userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new NotFoundException("User not found: " + username);
        }

        return Templates.home(username,
                              postRepository.findPaginatedNewestFromAuthor(username, limit, 1),
                              loggedUser);
    }
}