package dev.vepo.contraponto.rss;

import org.eclipse.microprofile.openapi.annotations.Operation;

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

    static final int FEED_LIMIT = RssFeedPaths.FEED_LIMIT;

    private final RssFeedService rssFeedService;

    @Inject
    public SiteWideFeedEndpoint(RssFeedService rssFeedService) {
        this.rssFeedService = rssFeedService;
    }

    @GET
    @Operation(hidden = true)
    @Produces("application/rss+xml;charset=UTF-8")
    public Response siteFeed(@Context UriInfo uriInfo) {
        return Response.ok(rssFeedService.siteWideFeed(uriInfo.getBaseUri().toString())).build();
    }
}
