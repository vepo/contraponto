package dev.vepo.contraponto.comment;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;

import dev.vepo.contraponto.custompage.CustomPageRepository;
import dev.vepo.contraponto.custompage.Links;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.shared.toast.Toast;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Logged
@Path("/comments")
@ApplicationScoped
public class CommentManageEndpoint {

    @CheckedTemplate
    public static class Templates {
        static native TemplateInstance list(List<CommentManageRow> comments, Links links, LoggedUser user);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final PostCommentRepository commentRepository;
    private final CustomPageRepository customPageRepository;
    private final LoggedUser loggedUser;

    @Inject
    public CommentManageEndpoint(PostCommentRepository commentRepository,
                                 CustomPageRepository customPageRepository,
                                 LoggedUser loggedUser) {
        this.commentRepository = commentRepository;
        this.customPageRepository = customPageRepository;
        this.loggedUser = loggedUser;
    }

    private Response forbidden() {
        return Toast.response(Response.Status.FORBIDDEN)
                    .message("You must be signed in to manage comments.")
                    .type(Toast.Type.ERROR)
                    .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                    .build();
    }

    @GET
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public Response list() {
        if (!loggedUser.isAuthenticated()) {
            return forbidden();
        }

        return Response.ok(renderList()).build();
    }

    TemplateInstance renderList() {
        var comments = commentRepository.findPendingForPostAuthor(loggedUser.getId())
                                        .stream()
                                        .map(CommentManageRow::from)
                                        .toList();
        return Templates.list(comments, customPageRepository.loadLinks(), loggedUser);
    }
}
