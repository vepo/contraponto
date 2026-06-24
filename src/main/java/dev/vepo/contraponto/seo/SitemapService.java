package dev.vepo.contraponto.seo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.blog.BlogPublicUrlService;
import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.custompage.CustomPage;
import dev.vepo.contraponto.custompage.CustomPageRepository;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostPublicationRepository;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.serie.Serie;
import dev.vepo.contraponto.serie.SerieRepository;
import dev.vepo.contraponto.post.PostTemplateExtensions;
import dev.vepo.contraponto.shared.qute.SharedTemplateExtensions;
import dev.vepo.contraponto.tag.Tag;
import dev.vepo.contraponto.directory.AuthorProfilePaths;
import dev.vepo.contraponto.tag.TagPaths;
import dev.vepo.contraponto.tag.TagRepository;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.user.UserRepository;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SitemapService {

    private static String escapeXml(String value) {
        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&apos;");
    }

    private final PublicSiteUrl publicSiteUrl;
    private final PostRepository postRepository;
    private final PostPublicationRepository publicationRepository;
    private final BlogRepository blogRepository;
    private final TagRepository tagRepository;
    private final CustomPageRepository customPageRepository;
    private final SerieRepository serieRepository;
    private final UserRepository userRepository;

    private final BlogPublicUrlService blogPublicUrlService;

    @Inject
    public SitemapService(PublicSiteUrl publicSiteUrl,
                          PostRepository postRepository,
                          PostPublicationRepository publicationRepository,
                          BlogRepository blogRepository,
                          TagRepository tagRepository,
                          CustomPageRepository customPageRepository,
                          SerieRepository serieRepository,
                          UserRepository userRepository,
                          BlogPublicUrlService blogPublicUrlService) {
        this.publicSiteUrl = publicSiteUrl;
        this.postRepository = postRepository;
        this.publicationRepository = publicationRepository;
        this.blogRepository = blogRepository;
        this.tagRepository = tagRepository;
        this.customPageRepository = customPageRepository;
        this.serieRepository = serieRepository;
        this.userRepository = userRepository;
        this.blogPublicUrlService = blogPublicUrlService;
    }

    public List<SitemapUrl> buildUrls() {
        Optional<LocalDateTime> siteLastModified = publicationRepository.findMaxPublishedAtSiteWide();
        List<SitemapUrl> urls = new ArrayList<>();
        urls.add(new SitemapUrl(platformLoc("/"), siteLastModified));
        urls.add(new SitemapUrl(platformLoc("/authors"), siteLastModified));
        urls.add(new SitemapUrl(platformLoc("/explore/blogs"), siteLastModified));
        for (User author : userRepository.findAuthorsWithPublishedPosts()) {
            urls.add(new SitemapUrl(platformLoc(AuthorProfilePaths.url(author)), Optional.ofNullable(author.getUpdatedAt())));
        }

        for (Post post : postRepository.findPublishedForSitemap()) {
            Optional<String> cover = Optional.ofNullable(PostTemplateExtensions.coverUrl(post));
            urls.add(new SitemapUrl(blogPublicUrlService.canonicalOrPlatformAbsolute(post), lastModified(post), cover));
        }
        for (Blog blog : blogRepository.findAllActiveWithOwner()) {
            urls.add(new SitemapUrl(blogPublicUrlService.canonicalOrPlatformAbsolute(blog),
                                    publicationRepository.findMaxPublishedAtByBlogId(blog.getId())));
        }
        for (Tag tag : tagRepository.listAllForManagement()) {
            if (hasPublishedPostsForTag(tag.getSlug())) {
                urls.add(new SitemapUrl(platformLoc(TagPaths.url(tag)), publicationRepository.findMaxPublishedAtByTagSlug(tag.getSlug())));
            }
        }
        for (Serie serie : serieRepository.findAllWithPublishedPosts()) {
            urls.add(new SitemapUrl(blogPublicUrlService.canonicalOrPlatformAbsolute(serie),
                                    publicationRepository.findMaxPublishedAtBySerieId(serie.getId())));
        }
        for (CustomPage page : customPageRepository.findPublishedForSitemap()) {
            urls.add(new SitemapUrl(blogPublicUrlService.canonicalOrPlatformAbsolute(page), Optional.ofNullable(page.getUpdatedAt())));
        }
        return urls;
    }

    private boolean hasPublishedPostsForTag(String slug) {
        return !postRepository.findPublishedFeedByTagSlug(slug, 1).isEmpty();
    }

    private Optional<LocalDateTime> lastModified(Post post) {
        if (post.getLivePublication() != null && post.getLivePublication().getPublishedAt() != null) {
            return Optional.of(post.getLivePublication().getPublishedAt());
        }
        return Optional.ofNullable(post.getPublishedAt());
    }

    private String platformLoc(String path) {
        return publicSiteUrl.absolute(path);
    }

    @CacheResult(cacheName = "sitemap")
    public String renderXml() {
        var builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        builder.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\"");
        builder.append(" xmlns:image=\"http://www.google.com/schemas/sitemap-image/1.1\">\n");
        for (SitemapUrl url : buildUrls()) {
            builder.append("  <url>\n");
            builder.append("    <loc>").append(escapeXml(url.loc())).append("</loc>\n");
            url.lastModified().ifPresent(lastmod -> builder.append("    <lastmod>")
                                                           .append(escapeXml(lastmod.toLocalDate().toString()))
                                                           .append("</lastmod>\n"));
            url.imagePath().ifPresent(imagePath -> {
                builder.append("    <image:image>\n");
                builder.append("      <image:loc>").append(escapeXml(publicSiteUrl.absolute(imagePath))).append("</image:loc>\n");
                builder.append("    </image:image>\n");
            });
            builder.append("  </url>\n");
        }
        builder.append("</urlset>");
        return builder.toString();
    }
}
