package dev.vepo.contraponto.seo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.blog.BlogEndpoint;
import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.custompage.CustomPage;
import dev.vepo.contraponto.custompage.CustomPagePaths;
import dev.vepo.contraponto.custompage.CustomPageRepository;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostEndpoint;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.serie.Serie;
import dev.vepo.contraponto.serie.SeriePageEndpoint;
import dev.vepo.contraponto.shared.infra.TemplateExtensions;
import dev.vepo.contraponto.tag.Tag;
import dev.vepo.contraponto.directory.AuthorProfileEndpoint;
import dev.vepo.contraponto.tag.TagPageEndpoint;
import dev.vepo.contraponto.tag.TagRepository;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.user.UserRepository;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

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
    private final BlogRepository blogRepository;
    private final TagRepository tagRepository;
    private final CustomPageRepository customPageRepository;

    private final EntityManager entityManager;
    private final UserRepository userRepository;

    @Inject
    public SitemapService(PublicSiteUrl publicSiteUrl,
                          PostRepository postRepository,
                          BlogRepository blogRepository,
                          TagRepository tagRepository,
                          CustomPageRepository customPageRepository,
                          UserRepository userRepository,
                          EntityManager entityManager) {
        this.publicSiteUrl = publicSiteUrl;
        this.postRepository = postRepository;
        this.blogRepository = blogRepository;
        this.tagRepository = tagRepository;
        this.customPageRepository = customPageRepository;
        this.userRepository = userRepository;
        this.entityManager = entityManager;
    }

    public List<SitemapUrl> buildUrls() {
        Optional<LocalDateTime> siteLastModified = findSiteWideLastModified();
        List<SitemapUrl> urls = new ArrayList<>();
        urls.add(new SitemapUrl("/", siteLastModified));
        urls.add(new SitemapUrl("/authors", siteLastModified));
        urls.add(new SitemapUrl("/explore/blogs", siteLastModified));
        for (User author : userRepository.findAuthorsWithPublishedPosts()) {
            urls.add(new SitemapUrl(AuthorProfileEndpoint.url(author), Optional.ofNullable(author.getUpdatedAt())));
        }

        for (Post post : postRepository.findPublishedForSitemap()) {
            Optional<String> cover = Optional.ofNullable(TemplateExtensions.coverUrl(post));
            urls.add(new SitemapUrl(PostEndpoint.extractUrl(post), lastModified(post), cover));
        }
        for (Blog blog : blogRepository.findAllActiveWithOwner()) {
            urls.add(new SitemapUrl(BlogEndpoint.extractUrl(blog), findBlogLastModified(blog.getId())));
        }
        for (Tag tag : tagRepository.listAllForManagement()) {
            if (hasPublishedPostsForTag(tag.getSlug())) {
                urls.add(new SitemapUrl(TagPageEndpoint.url(tag), findTagLastModified(tag.getSlug())));
            }
        }
        for (Serie serie : findSeriesWithPublishedPosts()) {
            urls.add(new SitemapUrl(SeriePageEndpoint.extractUrl(serie), findSerieLastModified(serie.getId())));
        }
        for (CustomPage page : customPageRepository.findPublishedForSitemap()) {
            urls.add(new SitemapUrl(CustomPagePaths.publicUrl(page), Optional.ofNullable(page.getUpdatedAt())));
        }
        return urls;
    }

    private Optional<LocalDateTime> findBlogLastModified(long blogId) {
        return optionalMaxPublishedAt("""
                                      SELECT MAX(pub.publishedAt) FROM PostPublication pub
                                      JOIN pub.post p
                                      WHERE p.published = TRUE AND p.blog.id = :blogId
                                      """, "blogId", blogId);
    }

    private Optional<LocalDateTime> findSerieLastModified(long serieId) {
        return optionalMaxPublishedAt("""
                                      SELECT MAX(pub.publishedAt) FROM PostPublication pub
                                      JOIN pub.post p
                                      WHERE p.published = TRUE AND p.serie.id = :serieId
                                      """, "serieId", serieId);
    }

    @SuppressWarnings("unchecked")
    private List<Serie> findSeriesWithPublishedPosts() {
        return entityManager.createQuery("""
                                         SELECT DISTINCT s FROM Serie s
                                         JOIN FETCH s.blog b
                                         JOIN FETCH b.owner
                                         WHERE b.active = TRUE AND
                                               EXISTS (
                                                   SELECT 1 FROM Post p
                                                   WHERE p.serie = s AND p.published = TRUE
                                               )
                                         ORDER BY s.id
                                         """, Serie.class)
                            .getResultList();
    }

    private Optional<LocalDateTime> findSiteWideLastModified() {
        return optionalMaxPublishedAt("""
                                      SELECT MAX(pub.publishedAt) FROM PostPublication pub
                                      JOIN pub.post p
                                      JOIN p.blog b
                                      WHERE p.published = TRUE AND b.active = TRUE
                                      """, null, null);
    }

    private Optional<LocalDateTime> findTagLastModified(String tagSlug) {
        return optionalMaxPublishedAt("""
                                      SELECT MAX(pub.publishedAt) FROM PostPublication pub
                                      JOIN pub.post p
                                      JOIN p.blog b
                                      JOIN p.tags t
                                      WHERE p.published = TRUE AND b.active = TRUE AND t.slug = :tagSlug
                                      """, "tagSlug", tagSlug);
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

    private Optional<LocalDateTime> optionalMaxPublishedAt(String jpql, String paramName, Object paramValue) {
        var query = entityManager.createQuery(jpql, LocalDateTime.class);
        if (paramName != null) {
            query.setParameter(paramName, paramValue);
        }
        try {
            LocalDateTime result = query.getSingleResult();
            return Optional.ofNullable(result);
        } catch (NoResultException _) {
            return Optional.empty();
        }
    }

    @CacheResult(cacheName = "sitemap")
    public String renderXml() {
        var builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        builder.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\"");
        builder.append(" xmlns:image=\"http://www.google.com/schemas/sitemap-image/1.1\">\n");
        for (SitemapUrl url : buildUrls()) {
            builder.append("  <url>\n");
            builder.append("    <loc>").append(escapeXml(publicSiteUrl.absolute(url.path()))).append("</loc>\n");
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
