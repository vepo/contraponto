package dev.vepo.contraponto.git;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.blog.BlogAccess;
import dev.vepo.contraponto.blog.BlogRepository;
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

@Logged
@Path("/blogs/{blogId}/git-sync")
@ApplicationScoped
public class GitSyncHistoryEndpoint {

    @CheckedTemplate
    public static class Templates {
        static native TemplateInstance detail(Blog blog,
                                              GitSyncRun run,
                                              List<GitSyncRunEntry> entries,
                                              Links links,
                                              LoggedUser user,
                                              BreadcrumbTrail breadcrumb);

        static native TemplateInstance list(Blog blog,
                                            Page<GitSyncRun> runs,
                                            Links links,
                                            LoggedUser user,
                                            BreadcrumbTrail breadcrumb);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final BlogRepository blogRepository;
    private final BlogAccess blogAccess;
    private final GitSyncRunService gitSyncRunService;
    private final CustomPageRepository customPageRepository;
    private final LoggedUser loggedUser;
    private final BreadcrumbService breadcrumbService;

    @Inject
    public GitSyncHistoryEndpoint(BlogRepository blogRepository,
                                  BlogAccess blogAccess,
                                  GitSyncRunService gitSyncRunService,
                                  CustomPageRepository customPageRepository,
                                  LoggedUser loggedUser,
                                  BreadcrumbService breadcrumbService) {
        this.blogRepository = blogRepository;
        this.blogAccess = blogAccess;
        this.gitSyncRunService = gitSyncRunService;
        this.customPageRepository = customPageRepository;
        this.loggedUser = loggedUser;
        this.breadcrumbService = breadcrumbService;
    }

    @GET
    @Path("/{runId}")
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance detail(@PathParam("blogId") long blogId, @PathParam("runId") long runId) {
        Blog blog = requireEditableBlog(blogId);
        GitSyncRun run = gitSyncRunService.findForBlog(blogId, runId).orElseThrow(NotFoundException::new);
        List<GitSyncRunEntry> entries = gitSyncRunService.listEntries(runId);
        Links links = blog.isMain() ? customPageRepository.loadLinks() : customPageRepository.loadLinks(blog.getId());
        return Templates.detail(blog,
                                run,
                                entries,
                                links,
                                loggedUser,
                                breadcrumbService.writingBlogGitSyncRun(blog, "Git sync run %s".formatted(run.getId())));
    }

    @GET
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list(@PathParam("blogId") long blogId, @QueryParam("page") @DefaultValue("1") int page) {
        Blog blog = requireEditableBlog(blogId);
        Links links = blog.isMain() ? customPageRepository.loadLinks() : customPageRepository.loadLinks(blog.getId());
        Page<GitSyncRun> runs = gitSyncRunService.listForBlog(blogId, PageQuery.forGrid(20, page));
        return Templates.list(blog, runs, links, loggedUser, breadcrumbService.writingBlogGitSync(blog));
    }

    private Blog requireEditableBlog(long blogId) {
        Blog blog = blogRepository.findById(blogId).orElseThrow(NotFoundException::new);
        if (!blogAccess.canEdit(blog, loggedUser)) {
            throw new NotFoundException();
        }
        return blog;
    }
}
