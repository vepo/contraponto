package dev.vepo.contraponto.api;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

@Logged  // Requires authentication
@Path("/api/posts")
@ApplicationScoped
public class PostApiEndpoint {

    private final PostRepository postRepository;
    private final LoggedUser loggedUser;

    @Inject
    public PostApiEndpoint(PostRepository postRepository, LoggedUser loggedUser) {
        this.postRepository = postRepository;
        this.loggedUser = loggedUser;
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response deletePost(@PathParam("id") Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Post not found"));

        // Check if the current user is the author
        if (!post.getAuthor().getId().equals(loggedUser.getId())) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("You are not allowed to delete this post")
                    .build();
        }

        // Optionally, prevent deletion of published posts (only drafts)
        if (post.isPublished()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Published posts cannot be deleted. Unpublish first.")
                    .build();
        }

        // Delete the post
        postRepository.delete(id);

        // Return no content – HTMX will remove the target element
        return Response.ok().build();
    }
}