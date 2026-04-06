package dev.vepo.contraponto.post;

import java.util.List;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.image.Image;
import dev.vepo.contraponto.image.ImageRepository;
import dev.vepo.contraponto.shared.security.Secured;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.user.UserRepository;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/posts")
@Produces(MediaType.APPLICATION_JSON)
public class PostApiEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(PostApiEndpoint.class);

    @Inject
    PostRepository postRepository;

    @Inject
    ImageRepository imageRepository;

    @Inject
    JsonWebToken jwt;

    @Inject
    UserRepository userRepository;

    private User getCurrentUser() {
        String userId = jwt.getSubject();
        return userRepository.findById(Long.parseLong(userId));
    }

    @GET
    @Secured
    public Response getUserPosts() {
        User user = getCurrentUser();
        List<Post> posts = postRepository.findByAuthor(user.getName());
        return Response.ok(posts).build();
    }

    @GET
    @Path("/{id}")
    @Secured
    public Response getPost(@PathParam("id") Long id) {
        Post post = postRepository.findById(id).orElse(null);
        if (post == null) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(new ErrorResponse("Post not found"))
                           .build();
        }
        return Response.ok(post).build();
    }

    @POST
    @Secured
    public Response createPost(@Valid PostRequest request) {
        User user = getCurrentUser();

        // Check if slug already exists
        if (postRepository.slugExists(request.slug(), null)) {
            return Response.status(Response.Status.CONFLICT)
                           .entity(new ErrorResponse("Slug already exists"))
                           .build();
        }

        // Get cover image if provided
        Image cover = null;
        if (request.coverId() != null) {
            cover = imageRepository.findById(request.coverId()).orElse(null);
            if (cover == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                               .entity(new ErrorResponse("Cover image not found"))
                               .build();
            }
        }

        Post post = postRepository.create(request, user.getName(), cover);
        return Response.ok(post).status(Response.Status.CREATED).build();
    }

    @PUT
    @Path("/{id}")
    @Secured
    public Response updatePost(@PathParam("id") Long id, @Valid PostRequest request) {
        User user = getCurrentUser();

        Post existingPost = postRepository.findById(id).orElse(null);
        if (existingPost == null) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(new ErrorResponse("Post not found"))
                           .build();
        }

        // Check if user is the author
        if (!existingPost.getAuthor().equals(user.getName())) {
            return Response.status(Response.Status.FORBIDDEN)
                           .entity(new ErrorResponse("You can only edit your own posts"))
                           .build();
        }

        // Check if slug already exists (excluding current post)
        if (postRepository.slugExists(request.slug(), id)) {
            return Response.status(Response.Status.CONFLICT)
                           .entity(new ErrorResponse("Slug already exists"))
                           .build();
        }

        // Get cover image if provided
        Image cover = null;
        if (request.coverId() != null) {
            cover = imageRepository.findById(request.coverId()).orElse(null);
            if (cover == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                               .entity(new ErrorResponse("Cover image not found"))
                               .build();
            }
        }

        Post post = postRepository.update(id, request, cover);
        return Response.ok(post).build();
    }

    @DELETE
    @Path("/{id}")
    @Secured
    public Response deletePost(@PathParam("id") Long id) {
        User user = getCurrentUser();

        Post post = postRepository.findById(id).orElse(null);
        if (post == null) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(new ErrorResponse("Post not found"))
                           .build();
        }

        // Check if user is the author
        if (!post.getAuthor().equals(user.getName())) {
            return Response.status(Response.Status.FORBIDDEN)
                           .entity(new ErrorResponse("You can only delete your own posts"))
                           .build();
        }

        boolean deleted = postRepository.delete(id);
        if (deleted) {
            return Response.noContent().build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity(new ErrorResponse("Failed to delete post"))
                           .build();
        }
    }

    record ErrorResponse(String error) {}
}