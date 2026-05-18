package dev.vepo.contraponto.custompage;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.blog.BlogRepository;
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
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Logged
@Path("/pages")
@ApplicationScoped
public class CustomPageManageEndpoint {

    @CheckedTemplate
    public static class Templates {
        static native TemplateInstance form(CustomPageFormView formView);

        static native TemplateInstance list(Page<CustomPageRow> pages, boolean editorView, Links links, LoggedUser user);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final CustomPageRepository customPageRepository;
    private final CustomPageAccess customPageAccess;
    private final BlogRepository blogRepository;
    private final LoggedUser loggedUser;

    @Inject
    public CustomPageManageEndpoint(CustomPageRepository customPageRepository,
                                    CustomPageAccess customPageAccess,
                                    BlogRepository blogRepository,
                                    LoggedUser loggedUser) {
        this.customPageRepository = customPageRepository;
        this.customPageAccess = customPageAccess;
        this.blogRepository = blogRepository;
        this.loggedUser = loggedUser;
    }

    private List<Blog> availableBlogs(boolean editorView) {
        if (editorView) {
            return blogRepository.findAllActiveWithOwner();
        }
        return blogRepository.findActiveBlogs(loggedUser.getId());
    }

    @GET
    @Path("{id}/edit")
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public Response edit(@PathParam("id") long id) {
        if (!loggedUser.isAuthenticated()) {
            return forbidden();
        }

        var page = customPageRepository.findByIdForManagement(id).orElseThrow(NotFoundException::new);
        if (!customPageAccess.canEdit(page, loggedUser)) {
            return forbidden();
        }

        var editorView = customPageAccess.canListAll(loggedUser);
        return Response.ok(Templates.form(new CustomPageFormView(page,
                                                                 CustomPagePaths.pathSlug(page.getSlug()),
                                                                 CustomPagePaths.publicUrl(page),
                                                                 availableBlogs(editorView),
                                                                 editorView,
                                                                 page.getBlog() == null)))
                       .build();
    }

    private Response forbidden() {
        return Toast.response(Response.Status.FORBIDDEN)
                    .message("You do not have permission to manage custom pages.")
                    .type(Toast.Type.ERROR)
                    .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                    .build();
    }

    private Links linksFor(CustomPage page) {
        if (page.getBlog() == null) {
            return customPageRepository.loadLinks();
        }
        return customPageRepository.loadLinks(page.getBlog().getId());
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response list(@QueryParam("page") @DefaultValue("1") int page) {
        if (!loggedUser.isAuthenticated()) {
            return forbidden();
        }

        var editorView = customPageAccess.canListAll(loggedUser);
        return Response.ok(Templates.list(listPage(page, editorView),
                                          editorView,
                                          customPageRepository.loadLinks(),
                                          loggedUser))
                       .build();
    }

    public Page<CustomPageRow> listPage(int page, boolean editorView) {
        var query = PageQuery.forGrid(20, page);
        return editorView ? customPageRepository.findPageAllForManagement(query)
                          : customPageRepository.findPageByOwnerId(loggedUser.getId(), query);
    }

    @GET
    @Path("new")
    @Produces(MediaType.TEXT_HTML)
    public Response newPage() {
        if (!loggedUser.isAuthenticated()) {
            return forbidden();
        }

        var editorView = customPageAccess.canManageApplicationPages(loggedUser);
        var blog = editorView ? null
                              : blogRepository.findMainByOwnerId(loggedUser.getId()).orElseThrow(NotFoundException::new);
        var page = customPageRepository.newPage(blog);

        return Response.ok(Templates.form(new CustomPageFormView(page,
                                                                 CustomPagePaths.pathSlug(page.getSlug()),
                                                                 editorView ? "" : CustomPagePaths.publicUrl(page),
                                                                 availableBlogs(editorView),
                                                                 editorView,
                                                                 editorView)))
                       .build();
    }
}
