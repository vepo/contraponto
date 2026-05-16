package dev.vepo.contraponto.blog;

import org.eclipse.microprofile.openapi.annotations.Operation;

import dev.vepo.contraponto.custompage.CustomPageRepository;
import dev.vepo.contraponto.custompage.Links;
import dev.vepo.contraponto.notification.BlogAudienceComponentEndpoint;
import dev.vepo.contraponto.notification.BlogAudienceView;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import dev.vepo.contraponto.user.User;
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
public class BlogEndpoint {

    @CheckedTemplate
    public static class Templates {
        static native TemplateInstance featured(Post post);

        static native TemplateInstance grid(String username, Page<Post> posts, boolean ignoreFirst);

        public static native TemplateInstance home(User author,
                                                   Blog mainBlog,
                                                   Page<Post> posts,
                                                   Links links,
                                                   LoggedUser user,
                                                   BlogAudienceView audience);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    public static String extractUrl(Blog blog) {
        if (blog.isMain()) {
            return "/%s".formatted(blog.getOwner().getUsername());
        } else {
            return "/%s/%s".formatted(blog.getOwner().getUsername(), blog.getSlug());
        }
    }

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CustomPageRepository customPageRepository;
    private final BlogRepository blogRepository;
    private final LoggedUser loggedUser;
    private final BlogAudienceComponentEndpoint audienceComponentEndpoint;

    @Inject
    public BlogEndpoint(UserRepository userRepository,
                        PostRepository postRepository,
                        CustomPageRepository customPageRepository,
                        BlogRepository blogRepository,
                        BlogAudienceComponentEndpoint audienceComponentEndpoint,
                        LoggedUser loggedUser) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.customPageRepository = customPageRepository;
        this.blogRepository = blogRepository;
        this.audienceComponentEndpoint = audienceComponentEndpoint;
        this.loggedUser = loggedUser;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance blog(@PathParam("username") String username, @QueryParam("limit") @DefaultValue("12") int limit) {
        // Check if user exists
        var user = userRepository.findByUsername(username).orElseThrow(() -> new NotFoundException("User not found: " + username));

        var mainBlog = blogRepository.findMainByOwnerId(user.getId()).orElseThrow(NotFoundException::new);
        return Templates.home(user,
                              mainBlog,
                              postRepository.findPublishedByAuthor(user.getId(), PageQuery.forFeaturedGrid(limit, 1)),
                              customPageRepository.loadLinks(mainBlog.getId()),
                              loggedUser,
                              audienceComponentEndpoint.buildView(mainBlog));
    }

    @GET
    @Path("components/home/grid")
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance morePosts(@PathParam("username") String username, @QueryParam("limit") @DefaultValue("12") int limit,
                                      @QueryParam("page") int page) {

        var user = userRepository.findByUsername(username).orElseThrow(() -> new NotFoundException("User not found: " + username));
        return Templates.grid(username,
                              this.postRepository.findPublishedByAuthor(user.getId(), PageQuery.forFeaturedGrid(limit, page)), false);
    }
}