package dev.vepo.contraponto.dashboard;

import java.util.List;
import java.util.Map;

import dev.vepo.contraponto.custompage.CustomPageRepository;
import dev.vepo.contraponto.custompage.Links;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.view.ViewRepository;
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

@Logged
@Path("/dashboard")
@ApplicationScoped
public class DashboardEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance analytics(DashboardAnalytics analytics);

        public static native TemplateInstance dashboard(DashboardPage page, Links links, LoggedUser user);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final PostRepository postRepository;
    private final ViewRepository viewRepository;
    private final CustomPageRepository customPageRepository;
    private final DashboardAnalyticsService analyticsService;
    private final LoggedUser loggedUser;

    @Inject
    public DashboardEndpoint(PostRepository postRepository,
                             ViewRepository viewRepository,
                             CustomPageRepository customPageRepository,
                             DashboardAnalyticsService analyticsService,
                             LoggedUser loggedUser) {
        this.postRepository = postRepository;
        this.viewRepository = viewRepository;
        this.customPageRepository = customPageRepository;
        this.analyticsService = analyticsService;
        this.loggedUser = loggedUser;
    }

    @GET
    @Path("components/analytics")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance analytics(@QueryParam("blogId") Long blogId,
                                      @QueryParam("year") Integer year,
                                      @QueryParam("month") Integer month,
                                      @QueryParam("compare") @DefaultValue("false") boolean compare) {
        return Templates.analytics(analyticsService.load(blogId, year, month, compare));
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance dashboard(@QueryParam("blogId") Long blogId) {
        var draftsCount = postRepository.countByAuthorAndPublished(loggedUser.getId(), false);
        var publishedCount = postRepository.countByAuthorAndPublished(loggedUser.getId(), true);
        var recentDrafts = postRepository.findRecentByAuthorAndPublished(loggedUser.getId(), false, 5);
        var recentPublished = postRepository.findRecentByAuthorAndPublished(loggedUser.getId(), true, 5);

        Map<Long, Long> viewCounts = viewRepository.getViewCountsForPosts(recentPublished.stream()
                                                                                         .map(Post::getId)
                                                                                         .toList());

        Long selectedBlogId = blogId != null ? blogId : analyticsService.resolveDefaultBlogId();

        var links = customPageRepository.loadLinks();
        return Templates.dashboard(new DashboardPage(draftsCount,
                                                     publishedCount,
                                                     recentDrafts,
                                                     recentPublished,
                                                     viewCounts,
                                                     selectedBlogId),
                                   links,
                                   loggedUser);
    }
}
