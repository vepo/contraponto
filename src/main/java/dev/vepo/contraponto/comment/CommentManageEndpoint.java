package dev.vepo.contraponto.comment;

import org.eclipse.microprofile.openapi.annotations.Operation;

import dev.vepo.contraponto.custompage.CustomPageRepository;
import dev.vepo.contraponto.custompage.Links;
import dev.vepo.contraponto.navigation.BreadcrumbService;
import dev.vepo.contraponto.navigation.BreadcrumbTrail;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import dev.vepo.contraponto.shared.toast.Toast;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

@Logged
@Path("/comments")
@ApplicationScoped
public class CommentManageEndpoint {

    @CheckedTemplate
    public static class Templates {
        static native TemplateInstance list(Page<CommentManageRow> comments,
                                            Links links,
                                            LoggedUser user,
                                            BreadcrumbTrail breadcrumb);

        static native TemplateInstance panel(Page<CommentManageRow> comments, String basePath);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final PostCommentRepository commentRepository;
    private final CustomPageRepository customPageRepository;
    private final LoggedUser loggedUser;
    private final BreadcrumbService breadcrumbService;

    @Inject
    public CommentManageEndpoint(PostCommentRepository commentRepository,
                                 CustomPageRepository customPageRepository,
                                 LoggedUser loggedUser,
                                 BreadcrumbService breadcrumbService) {
        this.commentRepository = commentRepository;
        this.customPageRepository = customPageRepository;
        this.loggedUser = loggedUser;
        this.breadcrumbService = breadcrumbService;
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
    public Response list(@QueryParam("page") @DefaultValue("1") int page) {
        if (!loggedUser.isAuthenticated()) {
            return forbidden();
        }

        return Response.seeOther(UriBuilder.fromPath("/manage/comments").queryParam("page", page).build()).build();
    }

    public TemplateInstance renderHubPanel(int page, String basePath) {
        var comments = commentRepository.findPendingPageForPostAuthor(loggedUser.getId(), PageQuery.forGrid(20, page))
                                        .map(CommentManageRow::from);
        return Templates.panel(comments, basePath);
    }

    TemplateInstance renderList(int page) {
        var comments = commentRepository.findPendingPageForPostAuthor(loggedUser.getId(), PageQuery.forGrid(20, page))
                                        .map(CommentManageRow::from);
        return Templates.list(comments, customPageRepository.loadLinks(), loggedUser, breadcrumbService.manageComments());
    }
}
