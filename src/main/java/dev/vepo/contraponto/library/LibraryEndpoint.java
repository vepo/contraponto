package dev.vepo.contraponto.library;

import dev.vepo.contraponto.custompage.CustomPageRepository;
import dev.vepo.contraponto.custompage.Links;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Logged
@Path("/library")
@ApplicationScoped
public class LibraryEndpoint {

    @CheckedTemplate
    public static class Templates {

        public static native TemplateInstance library(LoggedUser user, Links links);

        public static native TemplateInstance tab(Page<Post> posts, String type);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final PostRepository postRepository;
    private final CustomPageRepository customPageRepository;
    private final LoggedUser loggedUser;

    @Inject
    public LibraryEndpoint(PostRepository postRepository, CustomPageRepository customPageRepository, LoggedUser loggedUser) {
        this.postRepository = postRepository;
        this.customPageRepository = customPageRepository;
        this.loggedUser = loggedUser;
    }

    @DELETE
    @Path("components/posts/{postId}/delete")
    @Transactional
    public Response deletePost(@PathParam("postId") Long id) {
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

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance library() {
        return Templates.library(loggedUser, customPageRepository.loadLinks());
    }

    @GET
    @Path("components/tab/{type}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance tab(@PathParam("type") String type, @QueryParam("page") @DefaultValue("1") int page) {
        return Templates.tab(switch (type) {
            case "published" -> postRepository.findPageByAuthorAndPublished(loggedUser.getId(), true, PageQuery.forGrid(20, page));
            case "drafts" -> postRepository.findPageByAuthorAndPublished(loggedUser.getId(), false, PageQuery.forGrid(20, page));
            default -> throw new BadRequestException("Type not defined! type=%s".formatted(type));
        }, type);
    }
}