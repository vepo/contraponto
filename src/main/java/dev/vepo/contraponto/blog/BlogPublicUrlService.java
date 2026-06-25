package dev.vepo.contraponto.blog;

import dev.vepo.contraponto.custompage.CustomPage;
import dev.vepo.contraponto.custompage.CustomPagePaths;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostPaths;
import dev.vepo.contraponto.rss.RssFeedPaths;
import dev.vepo.contraponto.serie.Serie;
import dev.vepo.contraponto.serie.SeriePaths;
import dev.vepo.contraponto.user.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class BlogPublicUrlService {

    private static String shortPath(Blog blog) {
        if (blog.isMain()) {
            return "/";
        }
        return "/%s".formatted(blog.getSlug());
    }

    private static String shortPath(CustomPage page) {
        var slug = CustomPagePaths.pathSlug(page.getSlug());
        var blog = page.getBlog();
        if (blog.isMain()) {
            return "/page/%s".formatted(slug);
        }
        return "/%s/page/%s".formatted(blog.getSlug(), slug);
    }

    private static String shortPath(Post post) {
        var blog = post.getBlog();
        if (blog.isMain()) {
            return "/post/%s".formatted(post.getSlug());
        }
        return "/%s/post/%s".formatted(blog.getSlug(), post.getSlug());
    }

    private static String shortPath(Serie serie) {
        var blog = serie.getBlog();
        if (blog.isMain()) {
            return "/serie/%s".formatted(serie.getSlug());
        }
        return "/%s/serie/%s".formatted(blog.getSlug(), serie.getSlug());
    }

    private final BlogSubdomainConfig config;

    private final BlogSubdomainContext context;

    @Inject
    public BlogPublicUrlService(BlogSubdomainConfig config, BlogSubdomainContext context) {
        this.config = config;
        this.context = context;
    }

    public String absoluteCanonical(Blog blog) {
        if (blog == null || !config.enabled()) {
            return null;
        }
        var ownerUsername = blog.getOwner().getUsername();
        return "%s%s".formatted(config.subdomainOrigin(ownerUsername), shortPath(blog));
    }

    public String absoluteCanonical(CustomPage page) {
        if (page == null || page.getBlog() == null || !config.enabled()) {
            return null;
        }
        var blog = page.getBlog();
        var ownerUsername = blog.getOwner().getUsername();
        return "%s%s".formatted(config.subdomainOrigin(ownerUsername), shortPath(page));
    }

    public String absoluteCanonical(Post post) {
        if (post == null || !config.enabled()) {
            return null;
        }
        var ownerUsername = post.getBlog().getOwner().getUsername();
        return "%s%s".formatted(config.subdomainOrigin(ownerUsername), shortPath(post));
    }

    public String absoluteCanonical(Serie serie) {
        if (serie == null || !config.enabled()) {
            return null;
        }
        var blog = serie.getBlog();
        var ownerUsername = blog.getOwner().getUsername();
        return "%s%s".formatted(config.subdomainOrigin(ownerUsername), shortPath(serie));
    }

    public String applicationHomeUrl() {
        if (usesPlatformForWorkspaceLinks()) {
            return platformAbsolute("/");
        }
        return "/";
    }

    public boolean applicationHomeUsesHtmx() {
        return !usesPlatformForWorkspaceLinks();
    }

    public String authorBlogCanonical(User author) {
        if (author == null) {
            return null;
        }
        if (config.enabled()) {
            return config.subdomainOrigin(author.getUsername());
        }
        return platformAbsolute("/%s".formatted(author.getUsername()));
    }

    public String blogGridLoadMorePath(Blog blog) {
        if (blog == null) {
            return null;
        }
        var ownerUsername = blog.getOwner().getUsername();
        if (shouldUseShortPath(ownerUsername)) {
            if (blog.isMain()) {
                return "/components/grid";
            }
            return "/%s/components/grid".formatted(blog.getSlug());
        }
        if (blog.isMain()) {
            return "/%s/components/grid".formatted(ownerUsername);
        }
        return "/%s/%s/components/grid".formatted(ownerUsername, blog.getSlug());
    }

    public String canonicalOrPlatformAbsolute(Blog blog) {
        if (config.enabled()) {
            return absoluteCanonical(blog);
        }
        return platformAbsolute(BlogPaths.extractUrl(blog));
    }

    public String canonicalOrPlatformAbsolute(CustomPage page) {
        if (page == null) {
            return null;
        }
        if (page.getBlog() == null) {
            return platformAbsolute(CustomPagePaths.publicUrl(page));
        }
        if (config.enabled()) {
            return absoluteCanonical(page);
        }
        return platformAbsolute(CustomPagePaths.publicUrl(page));
    }

    public String canonicalOrPlatformAbsolute(Post post) {
        if (config.enabled()) {
            return absoluteCanonical(post);
        }
        return platformAbsolute(PostPaths.extractUrl(post));
    }

    public String canonicalOrPlatformAbsolute(Serie serie) {
        if (config.enabled()) {
            return absoluteCanonical(serie);
        }
        return platformAbsolute(SeriePaths.extractUrl(serie));
    }

    public String expandSubdomainPath(String path) {
        if (path == null || path.isBlank() || !context.onUserSubdomain()) {
            return path;
        }
        var username = context.subdomainUsername().orElse(null);
        if (username == null) {
            return path;
        }
        if ("/".equals(path) || path.isBlank()) {
            return "/%s".formatted(username);
        }
        if (path.startsWith("/")) {
            return "/%s%s".formatted(username, path);
        }
        return "/%s/%s".formatted(username, path);
    }

    public String mainBlogMenuUrl(User user) {
        if (user == null) {
            return "/";
        }
        if (context.onUserSubdomain()) {
            var subdomainAuthor = context.subdomainUsername().orElse("");
            if (user.getUsername().equals(subdomainAuthor)) {
                return "/";
            }
            return platformAbsolute("/%s".formatted(user.getUsername()));
        }
        return "/%s".formatted(user.getUsername());
    }

    public boolean mainBlogMenuUsesHtmx(User user) {
        if (user == null || !context.onUserSubdomain()) {
            return true;
        }
        return user.getUsername().equals(context.subdomainUsername().orElse(null));
    }

    public String platformAbsolute(String path) {
        return config.platformUrl(path);
    }

    public String relativeMainBlogHome(User author) {
        if (author == null) {
            return null;
        }
        if (shouldUseShortPath(author.getUsername())) {
            return "/";
        }
        if (context.onUserSubdomain() && !author.getUsername().equals(context.subdomainUsername().orElse(null))) {
            return config.subdomainOrigin(author.getUsername());
        }
        return "/%s".formatted(author.getUsername());
    }

    public String relativePath(Blog blog) {
        if (blog == null) {
            return null;
        }
        var ownerUsername = blog.getOwner().getUsername();
        if (shouldUseShortPath(ownerUsername)) {
            return shortPath(blog);
        }
        if (context.onUserSubdomain() && !ownerUsername.equals(context.subdomainUsername().orElse(null))) {
            return absoluteCanonical(blog);
        }
        return BlogPaths.extractUrl(blog);
    }

    public String relativePath(Post post) {
        if (post == null) {
            return null;
        }
        var ownerUsername = post.getBlog().getOwner().getUsername();
        if (shouldUseShortPath(ownerUsername)) {
            return shortPath(post);
        }
        if (context.onUserSubdomain() && !ownerUsername.equals(context.subdomainUsername().orElse(null))) {
            return absoluteCanonical(post);
        }
        return PostPaths.extractUrl(post);
    }

    public String rssFeedPath(Blog blog) {
        if (blog == null) {
            return null;
        }
        var ownerUsername = blog.getOwner().getUsername();
        if (shouldUseShortPath(ownerUsername)) {
            return subdomainFeedPath(blog);
        }
        return RssFeedPaths.blogFeed(blog);
    }

    private boolean shouldUseShortPath(String ownerUsername) {
        return context.onUserSubdomain() && ownerUsername.equals(context.subdomainUsername().orElse(null));
    }

    public String subdomainFeedPath(Blog blog) {
        if (blog == null) {
            return null;
        }
        if (blog.isMain()) {
            return "/feed/main-blog";
        }
        return "/%s/feed".formatted(blog.getSlug());
    }

    public boolean usesPlatformForWorkspaceLinks() {
        return context.onUserSubdomain();
    }

    public String workspaceMenuUrl(String path) {
        var normalized = path == null || path.isBlank() ? "/"
                              : path.startsWith("/") ? path
                              : "/%s".formatted(path);
        if (!usesPlatformForWorkspaceLinks()) {
            return normalized;
        }
        return platformAbsolute(normalized);
    }
}
