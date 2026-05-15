package dev.vepo.contraponto.blog;

import java.util.List;
import java.util.Optional;

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
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Logged
@Path("/blogs")
@ApplicationScoped
public class BlogManageEndpoint {

    @CheckedTemplate
    public static class Templates {
        static native TemplateInstance form(Optional<Blog> blog,
                                            String username,
                                            String publicUrl,
                                            boolean editorView,
                                            boolean canDelete,
                                            Links links,
                                            LoggedUser user);

        static native TemplateInstance list(List<BlogRow> blogs, boolean editorView, Links links, LoggedUser user);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final BlogRepository blogRepository;
    private final BlogAccess blogAccess;
    private final CustomPageRepository customPageRepository;
    private final LoggedUser loggedUser;

    @Inject
    public BlogManageEndpoint(BlogRepository blogRepository,
                              BlogAccess blogAccess,
                              CustomPageRepository customPageRepository,
                              LoggedUser loggedUser) {
        this.blogRepository = blogRepository;
        this.blogAccess = blogAccess;
        this.customPageRepository = customPageRepository;
        this.loggedUser = loggedUser;
    }

    @GET
    @Path("{id}/edit")
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public Response edit(@PathParam("id") long id) {
        if (!loggedUser.isAuthenticated()) {
            return forbidden();
        }

        var blog = blogRepository.findById(id).orElseThrow(NotFoundException::new);
        if (!blogAccess.canEdit(blog, loggedUser)) {
            return forbidden();
        }

        return Response.ok(Templates.form(Optional.of(blog),
                                          blog.getOwner().getUsername(),
                                          BlogEndpoint.extractUrl(blog),
                                          blogAccess.canListAll(loggedUser),
                                          blogAccess.canDelete(blog, loggedUser),
                                          customPageRepository.loadLinks(blog.getId()),
                                          loggedUser))
                       .build();
    }

    private Response forbidden() {
        return Toast.response(Response.Status.FORBIDDEN)
                    .message("You do not have permission to manage blogs.")
                    .type(Toast.Type.ERROR)
                    .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                    .build();
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response list() {
        if (!loggedUser.isAuthenticated()) {
            return forbidden();
        }

        var editorView = blogAccess.canListAll(loggedUser);
        var blogs = listRows(editorView);
        return Response.ok(Templates.list(blogs, editorView, customPageRepository.loadLinks(), loggedUser)).build();
    }

    private List<BlogRow> listRows(boolean editorView) {
        var blogs = editorView ? blogRepository.findAllForManagement()
                               : blogRepository.findByOwnerIdForManagement(loggedUser.getId());
        return blogs.stream().map(BlogRow::from).toList();
    }

    @GET
    @Path("new")
    @Produces(MediaType.TEXT_HTML)
    public Response newBlog() {
        if (!loggedUser.isAuthenticated() || !blogAccess.canCreate(loggedUser)) {
            return forbidden();
        }

        return Response.ok(Templates.form(Optional.empty(),
                                          loggedUser.getUsername(),
                                          "",
                                          blogAccess.canListAll(loggedUser),
                                          false,
                                          customPageRepository.loadLinks(),
                                          loggedUser))
                       .build();
    }
}
