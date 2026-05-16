package dev.vepo.contraponto.rss;

import org.eclipse.microprofile.openapi.annotations.Operation;

import dev.vepo.contraponto.post.PostRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * Dedicated resource so {@code GET /feed} matches before {@code /{username}}
 * blog routes.
 */
@Path("/feed")
@ApplicationScoped
public class SiteWideFeedEndpoint {

    static final int FEED_LIMIT = 50;

    private final PostRepository postRepository;

    @Inject
    public SiteWideFeedEndpoint(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @GET
    @Operation(hidden = true)
    @Produces("application/rss+xml;charset=UTF-8")
    public Response siteFeed(@Context UriInfo uriInfo) {
        var posts = postRepository.findPublishedFeedGlobal(FEED_LIMIT);
        var channel = new RssFeedRenderer.Channel("Contraponto", "/", "Recently published posts");
        return Response.ok(RssFeedRenderer.render(channel, posts, uriInfo.getBaseUri())).build();
    }
}
