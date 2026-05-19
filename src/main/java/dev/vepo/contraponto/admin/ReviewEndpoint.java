package dev.vepo.contraponto.admin;

import org.eclipse.microprofile.openapi.annotations.Operation;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import dev.vepo.contraponto.shared.i18n.I18nDefaults;
import dev.vepo.contraponto.shared.i18n.I18nKeys;
import dev.vepo.contraponto.shared.toast.Toast;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Logged
@Path("/editor/review/components")
@ApplicationScoped
public class ReviewEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance panel(Page<Post> posts, String basePath);

        public static native TemplateInstance row(Post post); // for HTMX swap

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final PostRepository postRepository;
    private final LoggedUser loggedUser;

    @Inject
    public ReviewEndpoint(PostRepository postRepository, LoggedUser loggedUser) {
        this.postRepository = postRepository;
        this.loggedUser = loggedUser;
    }

    private Response forbidden() {
        return Toast.response(Response.Status.FORBIDDEN)
                    .i18nKey(I18nKeys.TOAST_EDITOR_FORBIDDEN, I18nDefaults.EDITOR_FORBIDDEN)
                    .type(Toast.Type.ERROR)
                    .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                    .build();
    }

    public Page<Post> listPage(int page) {
        return postRepository.findPublished(PageQuery.forGrid(20, page));
    }

    @PUT
    @Path("{postId}/featured/toggle")
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    @Transactional
    public Response morePosts(@PathParam("postId") Long postId) {
        // ADMIN and EDITOR can select featured posts
        if (!loggedUser.isEditor()) {
            return Toast.response(Response.Status.FORBIDDEN)
                        .i18nKey(I18nKeys.TOAST_EDITOR_FORBIDDEN, I18nDefaults.EDITOR_FORBIDDEN)
                        .type(Toast.Type.ERROR)
                        .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                        .build();
        }

        var maybePost = postRepository.findById(postId);
        if (maybePost.isEmpty()) {
            return Toast.response(Response.Status.NOT_FOUND)
                        .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                        .i18nKey(I18nKeys.TOAST_POST_NOT_FOUND, I18nDefaults.POST_NOT_FOUND)
                        .build();
        }
        var post = maybePost.get();
        // Only published posts should be toggleable, but double-check
        if (!post.isPublished()) {
            return Toast.response(Response.Status.BAD_REQUEST)
                        .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                        .i18nKey(I18nKeys.TOAST_CANNOT_FEATURE_DRAFT, I18nDefaults.CANNOT_FEATURE_DRAFT)
                        .build();
        }
        post.setFeatured(!post.isFeatured());
        postRepository.save(post);
        return Response.ok()
                       .entity(Templates.row(post))
                       .build();
    }

    public TemplateInstance renderHubPanel(int page, String basePath) {
        return Templates.panel(listPage(page), basePath);
    }

}