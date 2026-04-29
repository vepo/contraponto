package dev.vepo.contraponto.admin;

import org.eclipse.microprofile.openapi.annotations.Operation;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import dev.vepo.contraponto.shared.toast.Toast;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Logged
@Path("/review")
@ApplicationScoped
public class ReviewEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance review(Page<Post> posts, LoggedUser user);

        public static native TemplateInstance grid(Page<Post> posts, LoggedUser user);

        public static native TemplateInstance row(Post post); // for HTMX swap
    }

    private final PostRepository postRepository;

    private final LoggedUser loggedUser;

    @Inject
    public ReviewEndpoint(PostRepository postRepository, LoggedUser loggedUser) {
        this.postRepository = postRepository;
        this.loggedUser = loggedUser;
    }

    /**
     * Main review page – shows all published posts with featured toggle.
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response review(@QueryParam("limit") @DefaultValue("12") int limit, @QueryParam("page") int page) {
        // ADMIN and EDITOR can select featured posts
        if (!loggedUser.isEditor()) {
            return Toast.response(Response.Status.FORBIDDEN)
                        .message("Usuário não possui permissões de editor!")
                        .type(Toast.Type.ERROR)
                        .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                        .build();
        }
        // Fetch all published posts, newest first
        return Response.ok()
                       .entity(Templates.review(postRepository.findPublished(PageQuery.forGrid(limit, 1)), loggedUser))
                       .build();
    }

    @GET
    @Path("components/page")
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public Response nextPage(@QueryParam("limit") @DefaultValue("12") int limit, @QueryParam("page") int page) {
        // ADMIN and EDITOR can select featured posts
        if (!loggedUser.isEditor()) {
            return Toast.response(Response.Status.FORBIDDEN)
                        .message("Usuário não possui permissões de editor!")
                        .type(Toast.Type.ERROR)
                        .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                        .build();
        }
        return Response.ok()
                       .entity(Templates.grid(postRepository.findPublished(PageQuery.forGrid(limit, page)), loggedUser))
                       .build();
    }

    @PUT
    @Path("components/{postId}/featured/toggle")
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    @Transactional
    public Response morePosts(@PathParam("postId") Long postId) {
        // ADMIN and EDITOR can select featured posts
        if (!loggedUser.isEditor()) {
            return Toast.response(Response.Status.FORBIDDEN)
                        .message("Usuário não possui permissões de editor!")
                        .type(Toast.Type.ERROR)
                        .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                        .build();
        }

        var maybePost = postRepository.findById(postId);
        if (maybePost.isEmpty()) {
            return Toast.response(Response.Status.NOT_FOUND)
                        .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                        .message("Post not found!")
                        .build();
        }
        var post = maybePost.get();
        // Only published posts should be toggleable, but double-check
        if (!post.isPublished()) {
            return Toast.response(Response.Status.BAD_REQUEST)
                        .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                        .message("Cannot feature a draft post")
                        .build();
        }
        post.setFeatured(!post.isFeatured());
        postRepository.save(post);
        return Response.ok()
                       .entity(Templates.row(post))
                       .build();
    }
}