package dev.vepo.contraponto.rss;

import org.eclipse.microprofile.openapi.annotations.Operation;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.blog.BlogEndpoint;
import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.serie.Serie;
import dev.vepo.contraponto.serie.SeriePageEndpoint;
import dev.vepo.contraponto.serie.SerieRepository;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.user.UserRepository;
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

    private static String descriptionOr(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback;
    }

    private final UserRepository userRepository;
    private final BlogRepository blogRepository;
    private final PostRepository postRepository;

    private final SerieRepository serieRepository;

    @Inject
    public UsernameRssEndpoint(UserRepository userRepository,
                               BlogRepository blogRepository,
                               PostRepository postRepository,
                               SerieRepository serieRepository) {
        this.userRepository = userRepository;
        this.blogRepository = blogRepository;
        this.postRepository = postRepository;
        this.serieRepository = serieRepository;
    }

    @GET
    @Path("{blogSlug}/feed")
    @Operation(hidden = true)
    @Produces("application/rss+xml;charset=UTF-8")
    public Response blogFeed(@PathParam("username") String username,
                             @PathParam("blogSlug") String blogSlug,
                             @Context UriInfo uriInfo) {
        Blog blog = blogRepository.findActiveByOwnerUsernameAndSlug(username, blogSlug).orElseThrow(NotFoundException::new);
        var posts = postRepository.findPublishedFeedByBlog(blog.getId(), FEED_LIMIT);
        var channel = new RssFeedRenderer.Channel(blog.getName(),
                                                  BlogEndpoint.extractUrl(blog),
                                                  descriptionOr(blog.getDescription(), blog.getName()));
        return Response.ok(RssFeedRenderer.render(channel, posts, uriInfo.getBaseUri())).build();
    }

    @GET
    @Path("feed/main-blog")
    @Operation(hidden = true)
    @Produces("application/rss+xml;charset=UTF-8")
    public Response mainBlogFeed(@PathParam("username") String username, @Context UriInfo uriInfo) {
        User user = requireUser(username);
        Blog main = blogRepository.findMainByOwnerId(user.getId()).orElseThrow(NotFoundException::new);
        var posts = postRepository.findPublishedFeedMainBlogByOwner(user.getId(), FEED_LIMIT);
        var channel = new RssFeedRenderer.Channel(main.getName(),
                                                  BlogEndpoint.extractUrl(main),
                                                  descriptionOr(main.getDescription(), main.getName()));
        return Response.ok(RssFeedRenderer.render(channel, posts, uriInfo.getBaseUri())).build();
    }

    @GET
    @Path("serie/{serieSlug}/feed")
    @Operation(hidden = true)
    @Produces("application/rss+xml;charset=UTF-8")
    public Response mainBlogSerieFeed(@PathParam("username") String username,
                                      @PathParam("serieSlug") String serieSlug,
                                      @Context UriInfo uriInfo) {
        Serie serie = serieRepository.findMainBlogSerie(username, serieSlug).orElseThrow(NotFoundException::new);
        return serieFeed(serie, uriInfo);
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> new NotFoundException("User not found: " + username));
    }

    @GET
    @Path("{blogSlug}/serie/{serieSlug}/feed")
    @Operation(hidden = true)
    @Produces("application/rss+xml;charset=UTF-8")
    public Response secondaryBlogSerieFeed(@PathParam("username") String username,
                                           @PathParam("blogSlug") String blogSlug,
                                           @PathParam("serieSlug") String serieSlug,
                                           @Context UriInfo uriInfo) {
        Serie serie = serieRepository.findSecondaryBlogSerie(username, blogSlug, serieSlug).orElseThrow(NotFoundException::new);
        return serieFeed(serie, uriInfo);
    }

    private Response serieFeed(Serie serie, UriInfo uriInfo) {
        var posts = postRepository.findPublishedFeedBySerie(serie.getId(), FEED_LIMIT);
        var channel = new RssFeedRenderer.Channel(serie.getTitle(),
                                                  SeriePageEndpoint.extractUrl(serie),
                                                  serie.getTitle());
        return Response.ok(RssFeedRenderer.render(channel, posts, uriInfo.getBaseUri())).build();
    }

    @GET
    @Path("feed")
    @Operation(hidden = true)
    @Produces("application/rss+xml;charset=UTF-8")
    public Response userFeed(@PathParam("username") String username, @Context UriInfo uriInfo) {
        User user = requireUser(username);
        var posts = postRepository.findPublishedFeedByAuthor(user.getId(), FEED_LIMIT);
        var channel =
                new RssFeedRenderer.Channel(user.getName(), "/" + username, "Posts by %s".formatted(user.getName()));
        return Response.ok(RssFeedRenderer.render(channel, posts, uriInfo.getBaseUri())).build();
    }
}
