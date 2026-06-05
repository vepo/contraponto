package dev.vepo.contraponto.rss;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.serie.Serie;
import dev.vepo.contraponto.tag.Tag;

public final class RssFeedPaths {

    public static final int FEED_LIMIT = 50;

    public static String blogFeed(Blog blog) {
        var owner = blog.getOwner();
        if (blog.isMain()) {
            return mainBlogFeed(owner.getUsername());
        }
        return blogFeed(owner.getUsername(), blog.getSlug());
    }

    public static String blogFeed(String username, String blogSlug) {
        return "/%s/%s/feed".formatted(username, blogSlug);
    }

    public static String mainBlogFeed(String username) {
        return "/%s/feed/main-blog".formatted(username);
    }

    public static String serieFeed(Serie serie) {
        var blog = serie.getBlog();
        var owner = blog.getOwner();
        if (blog.isMain()) {
            return "/%s/serie/%s/feed".formatted(owner.getUsername(), serie.getSlug());
        }
        return "/%s/%s/serie/%s/feed".formatted(owner.getUsername(), blog.getSlug(), serie.getSlug());
    }

    public static String siteFeed() {
        return "/feed";
    }

    public static String tagFeed(String tagSlug) {
        return "/tags/%s/feed".formatted(tagSlug);
    }

    public static String tagFeed(Tag tag) {
        return tagFeed(tag.getSlug());
    }

    private RssFeedPaths() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
