package dev.vepo.contraponto.library;

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
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.time.LocalDateTime;
import java.util.List;

@Logged
@Path("/library")
@ApplicationScoped
public class LibraryEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance library(LoggedUser user, int currentYear);

        public static native TemplateInstance postsList(List<Post> posts, String type);
    }

    private final PostRepository postRepository;
    private final LoggedUser loggedUser;

    @Inject
    public LibraryEndpoint(PostRepository postRepository, LoggedUser loggedUser) {
        this.postRepository = postRepository;
        this.loggedUser = loggedUser;
    }

    // Main library page
    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance library() {
        return Templates.library(loggedUser, LocalDateTime.now().getYear());
    }

    // Fragment endpoint for each tab – returns only the posts list
    @GET
    @Path("/tab")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance postsTab(@QueryParam("type") String type) {
        List<Post> posts;
        if ("published".equalsIgnoreCase(type)) {
            posts = postRepository.findByAuthorAndPublished(loggedUser.getUsername(), true);
        } else { // drafts
            posts = postRepository.findByAuthorAndPublished(loggedUser.getUsername(), false);
        }
        return Templates.postsList(posts, type);
    }
}