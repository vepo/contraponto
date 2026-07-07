package dev.vepo.contraponto.components;

import dev.vepo.contraponto.messaging.MessageThreadAccess;
import dev.vepo.contraponto.messaging.MessageThreadRepository;
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
import jakarta.ws.rs.QueryParam;
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
    private final MessageThreadRepository messageThreadRepository;
    private final MessageThreadAccess messageThreadAccess;
    private final LoggedUser loggedUser;

    @Inject
    public ConfirmModalEndpoint(PostManagementService postManagementService,
                                MessageThreadRepository messageThreadRepository,
                                MessageThreadAccess messageThreadAccess,
                                LoggedUser loggedUser) {
        this.postManagementService = postManagementService;
        this.messageThreadRepository = messageThreadRepository;
        this.messageThreadAccess = messageThreadAccess;
        this.loggedUser = loggedUser;
    }

    @GET
    @Path("message-block/{blockedUserId}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance messageBlock(@PathParam("blockedUserId") long blockedUserId,
                                         @QueryParam("returnUrl") String returnUrl) {
        if (!loggedUser.isAuthenticated()) {
            throw new NotFoundException();
        }
        return Templates.confirmModal(ConfirmModalView.forMessageBlock(blockedUserId, returnUrl));
    }

    @GET
    @Path("message-close/{threadId}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance messageClose(@PathParam("threadId") long threadId) {
        requireThreadParticipant(threadId);
        return Templates.confirmModal(ConfirmModalView.forMessageThread(ConfirmModalAction.MESSAGE_CLOSE, threadId));
    }

    @GET
    @Path("message-flag/{threadId}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance messageFlag(@PathParam("threadId") long threadId) {
        requireThreadParticipant(threadId);
        return Templates.confirmModal(ConfirmModalView.forMessageThread(ConfirmModalAction.MESSAGE_FLAG, threadId));
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

    private void requireThreadParticipant(long threadId) {
        if (!loggedUser.isAuthenticated()) {
            throw new NotFoundException();
        }
        var thread = messageThreadRepository.findByIdForParticipant(threadId, loggedUser.getId())
                                            .orElseThrow(NotFoundException::new);
        if (!messageThreadAccess.canParticipate(thread, loggedUser)) {
            throw new NotFoundException();
        }
    }
}
