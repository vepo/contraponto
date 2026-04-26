package dev.vepo.contraponto.dashboard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.view.ViewRepository;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Logged
@Path("/dashboard")
@ApplicationScoped
public class DashboardEndpoint {

    @CheckedTemplate
    @SuppressWarnings("java:S1118")
    public static class Templates {
        public static native TemplateInstance dashboard(LoggedUser user,
                                                        long draftsCount,
                                                        long publishedCount,
                                                        List<Post> recentDrafts,
                                                        List<Post> recentPublished,
                                                        Map<Long, Long> viewCounts);
    }

    private final PostRepository postRepository;
    private final ViewRepository viewRepository;
    private final LoggedUser loggedUser;

    @Inject
    public DashboardEndpoint(PostRepository postRepository, ViewRepository viewRepository, LoggedUser loggedUser) {
        this.postRepository = postRepository;
        this.viewRepository = viewRepository;
        this.loggedUser = loggedUser;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance dashboard() {
        long draftsCount = postRepository.countByAuthorAndPublished(loggedUser.getId(), false);
        long publishedCount = postRepository.countByAuthorAndPublished(loggedUser.getId(), true);
        List<Post> recentDrafts = postRepository.findRecentByAuthorAndPublished(loggedUser.getId(), false, 5);
        List<Post> recentPublished = postRepository.findRecentByAuthorAndPublished(loggedUser.getId(), true, 5);

        // Fetch view counts for the published posts
        Map<Long, Long> viewCounts = new HashMap<>();
        if (!recentPublished.isEmpty()) {
            List<Object[]> counts = viewRepository.getViewCountsForPosts(recentPublished);
            for (Object[] row : counts) {
                Long postId = (Long) row[0];
                Long count = (Long) row[1];
                viewCounts.put(postId, count);
            }
        }

        return Templates.dashboard(loggedUser,
                                   draftsCount,
                                   publishedCount,
                                   recentDrafts,
                                   recentPublished,
                                   viewCounts);
    }
}