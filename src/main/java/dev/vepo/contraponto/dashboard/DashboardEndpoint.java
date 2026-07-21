package dev.vepo.contraponto.dashboard;

import java.util.List;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.user.LoggedUser;
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
@Path("/manage/dashboard/components")
@ApplicationScoped
public class DashboardEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance analytics(DashboardAnalytics analytics);

        public static native TemplateInstance panel(DashboardPage page);

        public static native TemplateInstance scheduled(List<Post> scheduledPosts);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private static final int UPCOMING_SCHEDULED_LIMIT = 5;

    private final DashboardAnalyticsService analyticsService;
    private final PostRepository postRepository;
    private final LoggedUser loggedUser;

    @Inject
    public DashboardEndpoint(DashboardAnalyticsService analyticsService, PostRepository postRepository, LoggedUser loggedUser) {
        this.analyticsService = analyticsService;
        this.postRepository = postRepository;
        this.loggedUser = loggedUser;
    }

    @GET
    @Path("analytics")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance analytics(@QueryParam("blogId") Long blogId,
                                      @QueryParam("year") Integer year,
                                      @QueryParam("month") Integer month,
                                      @QueryParam("compare") @DefaultValue("false") boolean compare) {
        return Templates.analytics(analyticsService.load(blogId, year, month, compare));
    }

    @GET
    @Path("scheduled")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance scheduled() {
        return Templates.scheduled(postRepository.findUpcomingScheduledByAuthor(loggedUser.getId(), UPCOMING_SCHEDULED_LIMIT));
    }

}
