package dev.vepo.contraponto.blog;

import java.util.Optional;

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
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

@Logged
@Path("/blogs")
@ApplicationScoped
public class BlogManageEndpoint {

    @CheckedTemplate
    public static class Templates {
        static native TemplateInstance form(Optional<Blog> blog,
                                            String username,
                                            String publicUrl,
                                            BlogHubContext hubContext,
                                            boolean coreFormOnly,
                                            boolean canDelete,
                                            long uploadBlogId,
                                            Links links,
                                            LoggedUser user,
                                            BreadcrumbTrail breadcrumb);

        static native TemplateInstance list(Page<BlogRow> blogs,
                                            BlogHubContext hubContext,
                                            Links links,
                                            LoggedUser user,
                                            BreadcrumbTrail breadcrumb);

        static native TemplateInstance panel(Page<BlogRow> blogs,
                                             BlogHubContext hubContext,
                                             String basePath);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final BlogRepository blogRepository;
    private final BlogAccess blogAccess;
    private final CustomPageRepository customPageRepository;
    private final LoggedUser loggedUser;
    private final BreadcrumbService breadcrumbService;

    @Inject
    public BlogManageEndpoint(BlogRepository blogRepository,
                              BlogAccess blogAccess,
                              CustomPageRepository customPageRepository,
                              LoggedUser loggedUser,
                              BreadcrumbService breadcrumbService) {
        this.blogRepository = blogRepository;
        this.blogAccess = blogAccess;
        this.customPageRepository = customPageRepository;
        this.loggedUser = loggedUser;
        this.breadcrumbService = breadcrumbService;
    }

    private BreadcrumbTrail breadcrumbForEdit(Blog blog, BlogHubContext hubContext, boolean full) {
        if (hubContext == BlogHubContext.MANAGE) {
            return breadcrumbService.manageBlogEdit(blog);
        }
        if (full) {
            return breadcrumbService.writingBlogSettings(blog);
        }
        return breadcrumbService.writingBlogEdit(blog);
    }

    @GET
    @Path("{id}/edit")
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public Response edit(@PathParam("id") long id,
                         @QueryParam("hub") @DefaultValue("writing") String hub,
                         @QueryParam("full") @DefaultValue("false") boolean full) {
        if (!loggedUser.isAuthenticated()) {
            return forbidden();
        }

        var blog = blogRepository.findById(id).orElseThrow(NotFoundException::new);
        if (!blogAccess.canEdit(blog, loggedUser)) {
            return forbidden();
        }

        var hubContext = BlogHubContext.fromHubParam(hub);
        boolean coreFormOnly = hubContext.authorMode() && !full;
        return Response.ok(Templates.form(Optional.of(blog),
                                          blog.getOwner().getUsername(),
                                          BlogEndpoint.extractUrl(blog),
                                          hubContext,
                                          coreFormOnly,
                                          blogAccess.canDelete(blog, loggedUser),
                                          blog.getId(),
                                          customPageRepository.loadLinks(blog.getId()),
                                          loggedUser,
                                          breadcrumbForEdit(blog, hubContext, full)))
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
    public Response list(@QueryParam("page") @DefaultValue("1") int page) {
        if (!loggedUser.isAuthenticated()) {
            return forbidden();
        }

        return Response.seeOther(UriBuilder.fromPath("/writing/blogs").queryParam("page", page).build()).build();
    }

    public Page<BlogRow> listPage(int page, boolean editorView) {
        var query = PageQuery.forGrid(20, page);
        var blogs = editorView ? blogRepository.findPageAllForManagement(query)
                               : blogRepository.findPageByOwnerIdForManagement(loggedUser.getId(), query);
        return blogs.map(blog -> BlogRow.from(blog, blogAccess, loggedUser));
    }

    @GET
    @Path("new")
    @Produces(MediaType.TEXT_HTML)
    public Response newBlog(@QueryParam("hub") @DefaultValue("writing") String hub) {
        if (!loggedUser.isAuthenticated() || !blogAccess.canCreate(loggedUser)) {
            return forbidden();
        }

        var hubContext = BlogHubContext.fromHubParam(hub);
        if (hubContext == BlogHubContext.MANAGE) {
            return forbidden();
        }

        var mainBlogId = blogRepository.findMainByOwnerId(loggedUser.getId())
                                       .map(Blog::getId)
                                       .orElse(0L);
        return Response.ok(Templates.form(Optional.empty(),
                                          loggedUser.getUsername(),
                                          "",
                                          hubContext,
                                          true,
                                          false,
                                          mainBlogId,
                                          customPageRepository.loadLinks(),
                                          loggedUser,
                                          breadcrumbService.writingBlogNew()))
                       .build();
    }

    public TemplateInstance renderAuthorHubPanel(int page, String basePath) {
        return Templates.panel(listPage(page, false), BlogHubContext.WRITING, basePath);
    }

    public TemplateInstance renderPlatformHubPanel(int page, String basePath) {
        if (!blogAccess.canListAll(loggedUser)) {
            throw new NotFoundException("Platform blog management requires editor role.");
        }
        return Templates.panel(listPage(page, true), BlogHubContext.MANAGE, basePath);
    }

    @GET
    @Path("{id}/settings")
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public Response settings(@PathParam("id") long id) {
        return edit(id, BlogHubContext.WRITING.hubParam(), true);
    }
}
