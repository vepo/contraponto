package dev.vepo.contraponto.components;

import dev.vepo.contraponto.post.PostManagementService;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.user.LoggedUser;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Logged
@ApplicationScoped
@Path("/components/confirm-modal")
public class ConfirmModalEndpoint {

    @CheckedTemplate
    public static class Templates {

        public static native TemplateInstance confirmModal(ConfirmModalView view);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final PostManagementService postManagementService;
    private final LoggedUser loggedUser;

    @Inject
    public ConfirmModalEndpoint(PostManagementService postManagementService, LoggedUser loggedUser) {
        this.postManagementService = postManagementService;
        this.loggedUser = loggedUser;
    }

    @GET
    @Path("post-delete/{postId}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance postDelete(@PathParam("postId") long postId) {
        return render(ConfirmModalAction.POST_DELETE, postId);
    }

    @GET
    @Path("post-unpublish/{postId}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance postUnpublish(@PathParam("postId") long postId) {
        return render(ConfirmModalAction.POST_UNPUBLISH, postId);
    }

    private TemplateInstance render(ConfirmModalAction action, long postId) {
        if (!loggedUser.isAuthenticated()) {
            throw new NotFoundException("Post not found! id=%s".formatted(postId));
        }
        postManagementService.requireOwnedPost(postId, loggedUser);
        return Templates.confirmModal(ConfirmModalView.forPost(action, postId));
    }
}
