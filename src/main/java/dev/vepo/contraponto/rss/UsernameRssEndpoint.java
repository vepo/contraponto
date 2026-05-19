package dev.vepo.contraponto.rss;

import org.eclipse.microprofile.openapi.annotations.Operation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("{username}")
@ApplicationScoped
public class UsernameRssEndpoint {

    static final int FEED_LIMIT = 50;

    private final RssFeedService rssFeedService;

    @Inject
    public UsernameRssEndpoint(RssFeedService rssFeedService) {
        this.rssFeedService = rssFeedService;
    }

    @GET
    @Path("{blogSlug}/feed")
    @Operation(hidden = true)
    @Produces("application/rss+xml;charset=UTF-8")
    public Response blogFeed(@PathParam("username") String username,
                             @PathParam("blogSlug") String blogSlug,
                             @Context UriInfo uriInfo) {
        return Response.ok(rssFeedService.blogFeed(username, blogSlug, uriInfo.getBaseUri().toString())).build();
    }

    @GET
    @Path("feed/main-blog")
    @Operation(hidden = true)
    @Produces("application/rss+xml;charset=UTF-8")
    public Response mainBlogFeed(@PathParam("username") String username, @Context UriInfo uriInfo) {
        return Response.ok(rssFeedService.mainBlogFeed(username, uriInfo.getBaseUri().toString())).build();
    }

    @GET
    @Path("serie/{serieSlug}/feed")
    @Operation(hidden = true)
    @Produces("application/rss+xml;charset=UTF-8")
    public Response mainBlogSerieFeed(@PathParam("username") String username,
                                      @PathParam("serieSlug") String serieSlug,
                                      @Context UriInfo uriInfo) {
        return Response.ok(rssFeedService.mainBlogSerieFeed(username, serieSlug, uriInfo.getBaseUri().toString()))
                       .build();
    }

    @GET
    @Path("{blogSlug}/serie/{serieSlug}/feed")
    @Operation(hidden = true)
    @Produces("application/rss+xml;charset=UTF-8")
    public Response secondaryBlogSerieFeed(@PathParam("username") String username,
                                           @PathParam("blogSlug") String blogSlug,
                                           @PathParam("serieSlug") String serieSlug,
                                           @Context UriInfo uriInfo) {
        return Response.ok(rssFeedService.secondaryBlogSerieFeed(username,
                                                                 blogSlug,
                                                                 serieSlug,
                                                                 uriInfo.getBaseUri().toString()))
                       .build();
    }

    @GET
    @Path("feed")
    @Operation(hidden = true)
    @Produces("application/rss+xml;charset=UTF-8")
    public Response userFeed(@PathParam("username") String username, @Context UriInfo uriInfo) {
        return Response.ok(rssFeedService.userFeed(username, uriInfo.getBaseUri().toString())).build();
    }
}
