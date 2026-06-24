package dev.vepo.contraponto.rss;

import java.util.List;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.blog.BlogPublicUrlService;
import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.serie.Serie;
import dev.vepo.contraponto.serie.SerieRepository;
import dev.vepo.contraponto.tag.Tag;
import dev.vepo.contraponto.tag.TagRepository;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.shared.infra.SiteBranding;
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
    private final SiteBranding siteBranding;
    private final BlogPublicUrlService blogPublicUrlService;

    @Inject
    public RssFeedService(PostRepository postRepository,
                          UserRepository userRepository,
                          BlogRepository blogRepository,
                          SerieRepository serieRepository,
                          TagRepository tagRepository,
                          SiteBranding siteBranding,
                          BlogPublicUrlService blogPublicUrlService) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.blogRepository = blogRepository;
        this.serieRepository = serieRepository;
        this.tagRepository = tagRepository;
        this.siteBranding = siteBranding;
        this.blogPublicUrlService = blogPublicUrlService;
    }

    @CacheResult(cacheName = "rss-feeds")
    public String blogFeed(String username, String blogSlug, String baseUri) {
        Blog blog = blogRepository.findActiveByOwnerUsernameAndSlug(username, blogSlug)
                                  .orElseThrow(NotFoundException::new);
        return renderBlogFeed(blog);
    }

    @CacheResult(cacheName = "rss-feeds")
    public String mainBlogFeed(String username, String baseUri) {
        User user = requireUser(username);
        Blog main = blogRepository.findMainByOwnerId(user.getId()).orElseThrow(NotFoundException::new);
        return renderBlogFeed(main);
    }

    @CacheResult(cacheName = "rss-feeds")
    public String mainBlogSerieFeed(String username, String serieSlug, String baseUri) {
        Serie serie = serieRepository.findMainBlogSerie(username, serieSlug).orElseThrow(NotFoundException::new);
        return renderSerieFeed(serie);
    }

    private String renderBlogFeed(Blog blog) {
        var posts = postRepository.findPublishedFeedByBlog(blog.getId(), RssFeedPaths.FEED_LIMIT);
        var channel = new RssFeedRenderer.Channel(blog.getName(),
                                                  blogPublicUrlService.canonicalOrPlatformAbsolute(blog),
                                                  descriptionOr(blog.getDescription(), blog.getName()));
        return RssFeedRenderer.render(channel, posts, blogPublicUrlService::canonicalOrPlatformAbsolute);
    }

    private String renderSerieFeed(Serie serie) {
        List<Post> posts = postRepository.findPublishedFeedBySerie(serie.getId(), RssFeedPaths.FEED_LIMIT);
        var channel = new RssFeedRenderer.Channel(serie.getTitle(),
                                                  blogPublicUrlService.canonicalOrPlatformAbsolute(serie),
                                                  serie.getTitle());
        return RssFeedRenderer.render(channel, posts, blogPublicUrlService::canonicalOrPlatformAbsolute);
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> new NotFoundException("User not found: %s".formatted(username)));
    }

    @CacheResult(cacheName = "rss-feeds")
    public String secondaryBlogSerieFeed(String username, String blogSlug, String serieSlug, String baseUri) {
        Serie serie = serieRepository.findSecondaryBlogSerie(username, blogSlug, serieSlug)
                                     .orElseThrow(NotFoundException::new);
        return renderSerieFeed(serie);
    }

    @CacheResult(cacheName = "rss-feeds")
    public String siteWideFeed(String baseUri) {
        var posts = postRepository.findPublishedFeedGlobal(RssFeedPaths.FEED_LIMIT);
        var channel = new RssFeedRenderer.Channel(siteBranding.seoName(),
                                                  blogPublicUrlService.platformAbsolute("/"),
                                                  "Recently published posts");
        return RssFeedRenderer.render(channel, posts, blogPublicUrlService::canonicalOrPlatformAbsolute);
    }

    @CacheResult(cacheName = "rss-feeds")
    public String tagFeed(String tagSlug, String baseUri) {
        Tag tag = tagRepository.findBySlug(tagSlug)
                               .orElseThrow(() -> new NotFoundException("Tag not found: %s".formatted(tagSlug)));
        var posts = postRepository.findPublishedFeedByTagSlug(tag.getSlug(), RssFeedPaths.FEED_LIMIT);
        String desc = tag.getDescription();
        if (desc == null || desc.isBlank()) {
            desc = "Posts tagged %s".formatted(tag.getName());
        }
        var channel = new RssFeedRenderer.Channel(tag.getName(),
                                                  blogPublicUrlService.platformAbsolute("/tags/%s".formatted(tagSlug)),
                                                  desc);
        return RssFeedRenderer.render(channel, posts, blogPublicUrlService::canonicalOrPlatformAbsolute);
    }

    @CacheResult(cacheName = "rss-feeds")
    public String userFeed(String username, String baseUri) {
        User user = requireUser(username);
        var posts = postRepository.findPublishedFeedByAuthor(user.getId(), RssFeedPaths.FEED_LIMIT);
        var channel = new RssFeedRenderer.Channel(user.getName(),
                                                  blogPublicUrlService.authorBlogCanonical(user),
                                                  "Posts by %s".formatted(user.getName()));
        return RssFeedRenderer.render(channel, posts, blogPublicUrlService::canonicalOrPlatformAbsolute);
    }
}
