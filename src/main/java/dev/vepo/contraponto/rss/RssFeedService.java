package dev.vepo.contraponto.rss;

import java.net.URI;
import java.util.List;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.blog.BlogEndpoint;
import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.serie.Serie;
import dev.vepo.contraponto.serie.SeriePageEndpoint;
import dev.vepo.contraponto.serie.SerieRepository;
import dev.vepo.contraponto.tag.Tag;
import dev.vepo.contraponto.tag.TagRepository;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.user.UserRepository;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class RssFeedService {

    private static String descriptionOr(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback;
    }

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final BlogRepository blogRepository;
    private final SerieRepository serieRepository;

    private final TagRepository tagRepository;

    @Inject
    public RssFeedService(PostRepository postRepository,
                          UserRepository userRepository,
                          BlogRepository blogRepository,
                          SerieRepository serieRepository,
                          TagRepository tagRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.blogRepository = blogRepository;
        this.serieRepository = serieRepository;
        this.tagRepository = tagRepository;
    }

    @CacheResult(cacheName = "rss-feeds")
    public String blogFeed(String username, String blogSlug, String baseUri) {
        Blog blog = blogRepository.findActiveByOwnerUsernameAndSlug(username, blogSlug)
                                  .orElseThrow(NotFoundException::new);
        return renderBlogFeed(blog, baseUri);
    }

    @CacheResult(cacheName = "rss-feeds")
    public String mainBlogFeed(String username, String baseUri) {
        User user = requireUser(username);
        Blog main = blogRepository.findMainByOwnerId(user.getId()).orElseThrow(NotFoundException::new);
        return renderBlogFeed(main, baseUri);
    }

    @CacheResult(cacheName = "rss-feeds")
    public String mainBlogSerieFeed(String username, String serieSlug, String baseUri) {
        Serie serie = serieRepository.findMainBlogSerie(username, serieSlug).orElseThrow(NotFoundException::new);
        return renderSerieFeed(serie, baseUri);
    }

    private String renderBlogFeed(Blog blog, String baseUri) {
        var posts = postRepository.findPublishedFeedByBlog(blog.getId(), UsernameRssEndpoint.FEED_LIMIT);
        var channel = new RssFeedRenderer.Channel(blog.getName(),
                                                  BlogEndpoint.extractUrl(blog),
                                                  descriptionOr(blog.getDescription(), blog.getName()));
        return RssFeedRenderer.render(channel, posts, URI.create(baseUri));
    }

    private String renderSerieFeed(Serie serie, String baseUri) {
        List<Post> posts = postRepository.findPublishedFeedBySerie(serie.getId(), UsernameRssEndpoint.FEED_LIMIT);
        var channel = new RssFeedRenderer.Channel(serie.getTitle(),
                                                  SeriePageEndpoint.extractUrl(serie),
                                                  serie.getTitle());
        return RssFeedRenderer.render(channel, posts, URI.create(baseUri));
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> new NotFoundException("User not found: " + username));
    }

    @CacheResult(cacheName = "rss-feeds")
    public String secondaryBlogSerieFeed(String username, String blogSlug, String serieSlug, String baseUri) {
        Serie serie = serieRepository.findSecondaryBlogSerie(username, blogSlug, serieSlug)
                                     .orElseThrow(NotFoundException::new);
        return renderSerieFeed(serie, baseUri);
    }

    @CacheResult(cacheName = "rss-feeds")
    public String siteWideFeed(String baseUri) {
        var posts = postRepository.findPublishedFeedGlobal(SiteWideFeedEndpoint.FEED_LIMIT);
        var channel = new RssFeedRenderer.Channel("Contraponto", "/", "Recently published posts");
        return RssFeedRenderer.render(channel, posts, URI.create(baseUri));
    }

    @CacheResult(cacheName = "rss-feeds")
    public String tagFeed(String tagSlug, String baseUri) {
        Tag tag = tagRepository.findBySlug(tagSlug)
                               .orElseThrow(() -> new NotFoundException("Tag not found: " + tagSlug));
        var posts = postRepository.findPublishedFeedByTagSlug(tag.getSlug(), UsernameRssEndpoint.FEED_LIMIT);
        String desc = tag.getDescription();
        if (desc == null || desc.isBlank()) {
            desc = "Posts tagged %s".formatted(tag.getName());
        }
        var channel = new RssFeedRenderer.Channel(tag.getName(), "/tags/" + tagSlug, desc);
        return RssFeedRenderer.render(channel, posts, URI.create(baseUri));
    }

    @CacheResult(cacheName = "rss-feeds")
    public String userFeed(String username, String baseUri) {
        User user = requireUser(username);
        var posts = postRepository.findPublishedFeedByAuthor(user.getId(), UsernameRssEndpoint.FEED_LIMIT);
        var channel =
                new RssFeedRenderer.Channel(user.getName(), "/" + username, "Posts by %s".formatted(user.getName()));
        return RssFeedRenderer.render(channel, posts, URI.create(baseUri));
    }
}
