package dev.vepo.contraponto.image;

import org.eclipse.microprofile.openapi.annotations.Operation;

import java.util.List;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.blog.BlogAccess;
import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.dashboard.DashboardAnalyticsService;
import dev.vepo.contraponto.custompage.CustomPageRepository;
import dev.vepo.contraponto.custompage.Links;
import dev.vepo.contraponto.navigation.BreadcrumbService;
import dev.vepo.contraponto.navigation.BreadcrumbTrail;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
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
@Path("/blogs/{blogId}/images")
@ApplicationScoped
public class ImageControlEndpoint {

    @CheckedTemplate
    public static class Templates {
        static native TemplateInstance list(Blog blog,
                                            Page<ImageControlRow> images,
                                            Links links,
                                            LoggedUser user,
                                            BreadcrumbTrail breadcrumb);

        static native TemplateInstance panel(List<Blog> ownedBlogs,
                                             Blog blog,
                                             Page<ImageControlRow> images,
                                             String basePath,
                                             String extraQuery);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final BlogRepository blogRepository;
    private final BlogAccess blogAccess;
    private final ImageControlService imageControlService;
    private final CustomPageRepository customPageRepository;
    private final LoggedUser loggedUser;
    private final BreadcrumbService breadcrumbService;
    private final DashboardAnalyticsService dashboardAnalyticsService;

    @Inject
    public ImageControlEndpoint(BlogRepository blogRepository,
                                BlogAccess blogAccess,
                                ImageControlService imageControlService,
                                CustomPageRepository customPageRepository,
                                LoggedUser loggedUser,
                                BreadcrumbService breadcrumbService,
                                DashboardAnalyticsService dashboardAnalyticsService) {
        this.blogRepository = blogRepository;
        this.blogAccess = blogAccess;
        this.imageControlService = imageControlService;
        this.customPageRepository = customPageRepository;
        this.loggedUser = loggedUser;
        this.breadcrumbService = breadcrumbService;
        this.dashboardAnalyticsService = dashboardAnalyticsService;
    }

    public Response forbidden() {
        return Response.status(Response.Status.FORBIDDEN).build();
    }

    @GET
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list(@PathParam("blogId") long blogId, @QueryParam("page") @DefaultValue("1") int page) {
        Blog blog = blogRepository.findById(blogId).orElseThrow(NotFoundException::new);
        if (!blogAccess.canEdit(blog, loggedUser)) {
            throw new NotFoundException();
        }
        Links links = blog.isMain() ? customPageRepository.loadLinks() : customPageRepository.loadLinks(blog.getId());
        Page<ImageControlRow> images = imageControlService.listForBlog(blog, PageQuery.forGrid(20, page));
        return Templates.list(blog, images, links, loggedUser, breadcrumbService.writingBlogImages(blog));
    }

    public TemplateInstance renderHubPanel(Long blogId, int page) {
        List<Blog> ownedBlogs = blogRepository.findByOwnerIdForManagement(loggedUser.getId());
        if (ownedBlogs.isEmpty()) {
            return Templates.panel(List.of(), null, new Page<>(List.of(), 1, 20, 0), "/writing/images", "");
        }
        Long resolvedBlogId = blogId != null ? blogId : dashboardAnalyticsService.resolveDefaultBlogId();
        Blog blog = ownedBlogs.stream()
                              .filter(b -> b.getId().equals(resolvedBlogId))
                              .findFirst()
                              .orElse(ownedBlogs.getFirst());
        if (!blogAccess.canEdit(blog, loggedUser)) {
            throw new NotFoundException();
        }
        Page<ImageControlRow> images = imageControlService.listForBlog(blog, PageQuery.forGrid(20, page));
        String extraQuery = "&blogId=" + blog.getId();
        return Templates.panel(ownedBlogs, blog, images, "/writing/images", extraQuery);
    }
}
