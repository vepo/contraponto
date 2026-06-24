package dev.vepo.contraponto.blog;

import dev.vepo.contraponto.image.ImageDisplayWidth;
import dev.vepo.contraponto.rss.RssFeedPaths;
import io.quarkus.qute.TemplateExtension;
import jakarta.enterprise.inject.spi.CDI;

@TemplateExtension
public class BlogTemplateExtensions {

    @TemplateExtension
    public static String bannerUrl(Blog blog) {
        if (blog == null) {
            return null;
        }
        var service = CDI.current().select(BlogBannerService.class);
        if (!service.isResolvable()) {
            return null;
        }
        return service.get().effectiveBannerUrl(blog);
    }

    @TemplateExtension
    public static String blogGridLoadMorePath(Blog blog) {
        var service = selectPublicUrlService();
        if (service != null) {
            return service.blogGridLoadMorePath(blog);
        }
        if (blog.isMain()) {
            return blogGridLoadMorePath(blog.getOwner().getUsername());
        }
        return "/%s/%s/components/grid".formatted(blog.getOwner().getUsername(), blog.getSlug());
    }

    @TemplateExtension
    public static String blogGridLoadMorePath(String username) {
        return "/%s/components/grid".formatted(username);
    }

    @TemplateExtension
    public static String cardBannerUrl(Blog blog) {
        return sizedImageUrl(bannerUrl(blog), ImageDisplayWidth.BANNER);
    }

    @TemplateExtension
    public static String renderedDescription(Blog blog) {
        if (blog == null || blog.getDescription() == null || blog.getDescription().isBlank()) {
            return "";
        }
        return renderMarkdownDescription(blog.getDescription());
    }

    private static String renderMarkdownDescription(String description) {
        var renderer = CDI.current().select(BlogDescriptionRenderer.class);
        if (!renderer.isResolvable()) {
            return description;
        }
        return renderer.get().render(description);
    }

    @TemplateExtension
    public static String rssFeedUrl(Blog blog) {
        if (blog == null) {
            return null;
        }
        var service = selectPublicUrlService();
        return service != null ? service.rssFeedPath(blog) : RssFeedPaths.blogFeed(blog);
    }

    private static BlogPublicUrlService selectPublicUrlService() {
        try {
            var service = CDI.current().select(BlogPublicUrlService.class);
            return service.isResolvable() ? service.get() : null;
        } catch (IllegalStateException _) {
            return null;
        }
    }

    private static String sizedImageUrl(String url, ImageDisplayWidth width) {
        if (url == null || url.isBlank() || width == null) {
            return url;
        }
        if (!url.startsWith("/api/images/")) {
            return url;
        }
        return "%s?w=%d".formatted(url, width.pixels());
    }

    @TemplateExtension
    public static String url(Blog blog) {
        return CDI.current().select(BlogPublicUrlService.class).get().relativePath(blog);
    }

    private BlogTemplateExtensions() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
