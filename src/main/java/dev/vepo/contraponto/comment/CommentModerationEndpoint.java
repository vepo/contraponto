package dev.vepo.contraponto.comment;

import dev.vepo.contraponto.navigation.NavigationHub;
import dev.vepo.contraponto.navigation.NavigationHubService;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.shared.toast.Toast;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Logged
@ApplicationScoped
@Path("/forms/posts/{postId}/comments/{commentId}")
public class CommentModerationEndpoint {

    private final PostCommentService commentService;
    private final PostRepository postRepository;
    private final CommentComponentEndpoint componentEndpoint;
    private final NavigationHubService navigationHubService;
    private final LoggedUser loggedUser;

    @Inject
    public CommentModerationEndpoint(PostCommentService commentService,
                                     PostRepository postRepository,
                                     CommentComponentEndpoint componentEndpoint,
                                     NavigationHubService navigationHubService,
                                     LoggedUser loggedUser) {
        this.commentService = commentService;
        this.postRepository = postRepository;
        this.componentEndpoint = componentEndpoint;
        this.navigationHubService = navigationHubService;
        this.loggedUser = loggedUser;
    }

    @POST
    @Path("approve")
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    public Response approve(@PathParam("postId") long postId,
                            @PathParam("commentId") long commentId,
                            @QueryParam("from") String from) {
        return moderate(postId, commentId, true, from);
    }

    private Response moderate(long postId, long commentId, boolean approve, String from) {
        try {
            Post post = postRepository.findById(postId).orElseThrow(NotFoundException::new);
            if (approve) {
                commentService.approve(postId, commentId, loggedUser.getId());
            } else {
                commentService.reject(postId, commentId, loggedUser.getId());
            }
            String message = approve ? "Comment approved." : "Comment rejected.";
            var builder = Toast.ok()
                               .message(message)
                               .type(Toast.Type.SUCCESS)
                               .duration(Toast.TOAST_DEFAULT_DURATION_MS);
            if ("manage".equals(from)) {
                return builder.page(navigationHubService.shell(NavigationHub.MANAGE, "comments", 1)).build();
            }
            return builder.page(componentEndpoint.renderComments(post)).build();
        } catch (ForbiddenException e) {
            return Toast.response(Status.FORBIDDEN).message(e.getMessage()).type(Toast.Type.ERROR).build();
        } catch (NotFoundException _) {
            return Toast.response(Status.NOT_FOUND).message("Comment not found.").type(Toast.Type.ERROR).build();
        }
    }

    @POST
    @Path("reject")
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    public Response reject(@PathParam("postId") long postId,
                           @PathParam("commentId") long commentId,
                           @QueryParam("from") String from) {
        return moderate(postId, commentId, false, from);
    }
}
