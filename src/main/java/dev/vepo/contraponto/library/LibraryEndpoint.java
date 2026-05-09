package dev.vepo.contraponto.library;

import java.util.List;

import dev.vepo.contraponto.custompage.CustomPageRepository;
import dev.vepo.contraponto.custompage.Links;
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

@Logged
@Path("/library")
@ApplicationScoped
public class LibraryEndpoint {

    @CheckedTemplate
    public static class Templates {

        public static native TemplateInstance library(LoggedUser user, Links links);

        public static native TemplateInstance postsList(List<Post> posts, String type);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final PostRepository postRepository;
    private final CustomPageRepository customPageRepository;
    private final LoggedUser loggedUser;

    @Inject
    public LibraryEndpoint(PostRepository postRepository, CustomPageRepository customPageRepository, LoggedUser loggedUser) {
        this.postRepository = postRepository;
        this.customPageRepository = customPageRepository;
        this.loggedUser = loggedUser;
    }

    // Main library page
    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance library() {
        return Templates.library(loggedUser, customPageRepository.loadLinks());
    }

    // Fragment endpoint for each tab – returns only the posts list
    @GET
    @Path("/tab")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance postsTab(@QueryParam("type") String type) {
        List<Post> posts;
        if ("published".equalsIgnoreCase(type)) {
            posts = postRepository.findByAuthorAndPublished(loggedUser.getId(), true);
        } else { // drafts
            posts = postRepository.findByAuthorAndPublished(loggedUser.getId(), false);
        }
        return Templates.postsList(posts, type);
    }
}