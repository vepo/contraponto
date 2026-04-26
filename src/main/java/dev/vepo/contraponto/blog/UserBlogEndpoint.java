package dev.vepo.contraponto.blog;

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
        public static native TemplateInstance userBlog(String username, Page<Post> posts, LoggedUser user);
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
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance blog(@PathParam("username") String username,
                                 @DefaultValue("1") @QueryParam("page") int page) {
        // Check if user exists
        var userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new NotFoundException("User not found: " + username);
        }
        int limit = 10;
        return Templates.userBlog(username,
                                  postRepository.loadPaginatedAuthorPublishedPosts(username, page, limit),
                                  loggedUser);
    }
}