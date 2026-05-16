package dev.vepo.contraponto.comment;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.shared.toast.Toast;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Logged
@ApplicationScoped
@Path("/forms/posts/{postId}/comments")
public class CommentCreateEndpoint {

    private final PostCommentService commentService;
    private final PostRepository postRepository;
    private final CommentComponentEndpoint componentEndpoint;
    private final LoggedUser loggedUser;

    @Inject
    public CommentCreateEndpoint(PostCommentService commentService,
                                 PostRepository postRepository,
                                 CommentComponentEndpoint componentEndpoint,
                                 LoggedUser loggedUser) {
        this.commentService = commentService;
        this.postRepository = postRepository;
        this.componentEndpoint = componentEndpoint;
        this.loggedUser = loggedUser;
    }

    @POST
    @Path("{parentId}/replies")
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    public Response createReply(@PathParam("postId") long postId,
                                @PathParam("parentId") long parentId,
                                @FormParam("body") String body) {
        try {
            Post post = loadPost(postId);
            PostComment comment = commentService.createReply(postId, parentId, loggedUser.getId(), body);
            String message = comment.getStatus() == CommentStatus.APPROVED
                                                                           ? "Reply posted."
                                                                           : "Reply submitted and is awaiting approval.";
            return Toast.ok()
                        .message(message)
                        .type(Toast.Type.SUCCESS)
                        .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                        .page(componentEndpoint.renderComments(post))
                        .build();
        } catch (BadRequestException e) {
            return Toast.response(Status.BAD_REQUEST).message(e.getMessage()).type(Toast.Type.ERROR).build();
        } catch (NotFoundException e) {
            return Toast.response(Status.NOT_FOUND).message("Post or comment not found.").type(Toast.Type.ERROR).build();
        }
    }

    @POST
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    public Response createRoot(@PathParam("postId") long postId, @FormParam("body") String body) {
        try {
            Post post = loadPost(postId);
            commentService.createRootComment(postId, loggedUser.getId(), body);
            String message = post.getAuthor().getId().equals(loggedUser.getId())
                                                                                 ? "Comment posted."
                                                                                 : "Comment submitted and is awaiting approval.";
            return Toast.ok()
                        .message(message)
                        .type(Toast.Type.SUCCESS)
                        .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                        .page(componentEndpoint.renderComments(post))
                        .build();
        } catch (BadRequestException e) {
            return Toast.response(Status.BAD_REQUEST).message(e.getMessage()).type(Toast.Type.ERROR).build();
        } catch (NotFoundException e) {
            return Toast.response(Status.NOT_FOUND).message("Post not found.").type(Toast.Type.ERROR).build();
        }
    }

    private Post loadPost(long postId) {
        Post post = postRepository.findById(postId).orElseThrow(NotFoundException::new);
        if (!post.isPublished()) {
            throw new NotFoundException("Post not found.");
        }
        return post;
    }
}
