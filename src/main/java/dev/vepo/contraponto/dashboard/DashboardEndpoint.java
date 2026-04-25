package dev.vepo.contraponto.dashboard;

import java.time.LocalDateTime;
import java.util.List;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
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
    public static class Templates {
        public static native TemplateInstance dashboard(LoggedUser user,
                                                       int currentYear,
                                                       long draftsCount,
                                                       long publishedCount,
                                                       List<Post> recentDrafts,
                                                       List<Post> recentPublished);
    }

    private final PostRepository postRepository;
    private final LoggedUser loggedUser;

    @Inject
    public DashboardEndpoint(PostRepository postRepository, LoggedUser loggedUser) {
        this.postRepository = postRepository;
        this.loggedUser = loggedUser;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance dashboard() {
        long draftsCount = postRepository.countByAuthorAndPublished(loggedUser.getId(), false);
        long publishedCount = postRepository.countByAuthorAndPublished(loggedUser.getId(), true);
        List<Post> recentDrafts = postRepository.findRecentByAuthorAndPublished(loggedUser.getId(), false, 5);
        List<Post> recentPublished = postRepository.findRecentByAuthorAndPublished(loggedUser.getId(), true, 5);

        return Templates.dashboard(loggedUser,
                                   LocalDateTime.now().getYear(),
                                   draftsCount,
                                   publishedCount,
                                   recentDrafts,
                                   recentPublished);
    }
}