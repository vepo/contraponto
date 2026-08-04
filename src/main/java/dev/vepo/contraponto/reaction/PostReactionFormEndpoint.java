package dev.vepo.contraponto.reaction;

import java.util.Locale;

import dev.vepo.contraponto.activitypub.inbox.ActivityPubFavouriteService;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.user.LoggedUser;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Logged
@ApplicationScoped
@Path("/forms/posts/{postId}/reactions")
public class PostReactionFormEndpoint {

    private final PostRepository postRepository;
    private final PostReactionService reactionService;
    private final ActivityPubFavouriteService favouriteService;
    private final LoggedUser loggedUser;

    @Inject
    public PostReactionFormEndpoint(PostRepository postRepository,
                                    PostReactionService reactionService,
                                    ActivityPubFavouriteService favouriteService,
                                    LoggedUser loggedUser) {
        this.postRepository = postRepository;
        this.reactionService = reactionService;
        this.favouriteService = favouriteService;
        this.loggedUser = loggedUser;
    }

    private ReactionType parseType(String type) {
        try {
            return ReactionType.valueOf(type.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new NotFoundException("Unknown reaction type: " + type);
        }
    }

    @POST
    @Path("{type}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance toggle(@PathParam("postId") long postId, @PathParam("type") String type) {
        Post post = postRepository.findByIdWithBlog(postId).orElseThrow(NotFoundException::new);
        ReactionType reactionType = parseType(type);
        PostReactionBarView view = reactionService.toggle(post, loggedUser.getUser(), reactionType);
        var favourites = favouriteService.buildPostView(post);
        if (favourites.visible()) {
            view = view.withAdditionalLikeCount(favourites.count());
        }
        return PostReactionComponentEndpoint.Templates.reactionBar(view);
    }
}
