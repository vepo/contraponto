package dev.vepo.contraponto.reaction;

import dev.vepo.contraponto.activitypub.inbox.ActivityPubFavouriteService;
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
public class PostReactionComponentEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance reactionBar(PostReactionBarView view);

        private Templates() {
            throw new UnsupportedOperationException("Utility class");
        }
    }

    private final PostRepository postRepository;
    private final PostReactionService reactionService;
    private final ActivityPubFavouriteService favouriteService;
    private final LoggedUser loggedUser;

    @Inject
    public PostReactionComponentEndpoint(PostRepository postRepository,
                                         PostReactionService reactionService,
                                         ActivityPubFavouriteService favouriteService,
                                         LoggedUser loggedUser) {
        this.postRepository = postRepository;
        this.reactionService = reactionService;
        this.favouriteService = favouriteService;
        this.loggedUser = loggedUser;
    }

    @GET
    @Path("{blogSlug}/post/{slug}/components/reactions")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance blogReactions(@PathParam("username") String username,
                                          @PathParam("blogSlug") String blogSlug,
                                          @PathParam("slug") String slug) {
        Post post = postRepository.findBlogPost(username, blogSlug, slug).orElseThrow(NotFoundException::new);
        return renderReactionBar(post);
    }

    @GET
    @Path("post/{slug}/components/reactions")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance mainBlogReactions(@PathParam("username") String username, @PathParam("slug") String slug) {
        Post post = postRepository.findMainBlogPost(username, slug).orElseThrow(NotFoundException::new);
        return renderReactionBar(post);
    }

    public TemplateInstance renderReactionBar(Post post) {
        Long userId = loggedUser.isAuthenticated() ? loggedUser.getId() : null;
        PostReactionBarView view = reactionService.buildView(post, userId);
        var favourites = favouriteService.buildPostView(post);
        if (favourites.visible()) {
            view = view.withAdditionalLikeCount(favourites.count());
        }
        return Templates.reactionBar(view);
    }
}
