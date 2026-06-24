package dev.vepo.contraponto.readinglist;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.user.LoggedUser;
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

@ApplicationScoped
@Path("{username}")
public class ReadingListComponentEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance readingListAction(ReadingListActionView view);

        private Templates() {
            throw new UnsupportedOperationException("Utility class");
        }
    }

    private final PostRepository postRepository;
    private final ReadingListService readingListService;
    private final LoggedUser loggedUser;

    @Inject
    public ReadingListComponentEndpoint(PostRepository postRepository,
                                        ReadingListService readingListService,
                                        LoggedUser loggedUser) {
        this.postRepository = postRepository;
        this.readingListService = readingListService;
        this.loggedUser = loggedUser;
    }

    @GET
    @Path("{blogSlug}/post/{slug}/components/reading-list")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance blogReadingList(@PathParam("username") String username,
                                            @PathParam("blogSlug") String blogSlug,
                                            @PathParam("slug") String slug) {
        Post post = postRepository.findBlogPost(username, blogSlug, slug).orElseThrow(NotFoundException::new);
        return renderReadingList(post);
    }

    @GET
    @Path("post/{slug}/components/reading-list")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance mainBlogReadingList(@PathParam("username") String username, @PathParam("slug") String slug) {
        Post post = postRepository.findMainBlogPost(username, slug).orElseThrow(NotFoundException::new);
        return renderReadingList(post);
    }

    public TemplateInstance renderReadingList(Post post) {
        Long userId = loggedUser.isAuthenticated() ? loggedUser.getId() : null;
        ReadingListActionView view = readingListService.buildActionView(post, userId);
        return Templates.readingListAction(view);
    }
}
