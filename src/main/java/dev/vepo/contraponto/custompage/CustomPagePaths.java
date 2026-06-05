package dev.vepo.contraponto.custompage;

import java.util.Set;

import dev.vepo.contraponto.blog.Blog;

public final class CustomPagePaths {

    /**
     * First-path segments that must not be used as usernames or blog slugs, and
     * that must not be mistaken for application routes. Post and custom-page slugs
     * may still use these words (e.g. {@code /alice/post/js},
     * {@code /alice/page/style}).
     */
    private static final Set<String> RESERVED_SEGMENTS = Set.of("components",
                                                                "js",
                                                                "style",
                                                                "images",
                                                                "i18n",
                                                                "explore",
                                                                "feed",
                                                                "authors",
                                                                "main-blog",
                                                                "forms",
                                                                "api",
                                                                "write",
                                                                "writing",
                                                                "manage",
                                                                "account",
                                                                "editor",
                                                                "administration",
                                                                "search",
                                                                "library",
                                                                "dashboard",
                                                                "profile",
                                                                "review",
                                                                "pages",
                                                                "blogs",
                                                                "users",
                                                                "comments",
                                                                "notifications",
                                                                "subscriptions",
                                                                "tags",
                                                                "post",
                                                                "serie",
                                                                "auth",
                                                                "_custom_page");

    public static String blogSlug(java.util.List<jakarta.ws.rs.core.PathSegment> segments) {
        return segments.get(1).getPath();
    }

    public static String internalUrl(PageType type, String... segments) {
        var builder = new StringBuilder("/_custom_page");
        switch (type) {
            case GLOBAL -> builder.append("/global/").append(segments[0]);
            case USER -> builder.append("/user/").append(segments[0]).append('/').append(segments[1]);
            case BLOG -> builder.append("/blog/")
                                .append(segments[0])
                                .append('/')
                                .append(segments[1])
                                .append('/')
                                .append(segments[2]);
            default -> throw new IllegalArgumentException("Unsupported page type: %s".formatted(type));
        }
        return builder.toString();
    }

    public static boolean isMainBlogPage(CustomPage page) {
        Blog blog = page.getBlog();
        return blog != null && blog.isMain();
    }

    public static boolean isReservedSegment(String segment) {
        return RESERVED_SEGMENTS.contains(segment);
    }

    public static long linksBlogId(CustomPage page) {
        var blog = page.getBlog();
        if (blog == null) {
            throw new IllegalStateException("Application pages do not have a blog");
        }
        return blog.getId();
    }

    public static PageType matchPageType(java.util.List<jakarta.ws.rs.core.PathSegment> segments) {
        if (segments == null || segments.isEmpty()) {
            return PageType.NONE;
        }

        if (segments.size() >= 4 && "page".equals(segments.get(2).getPath())) {
            var username = segments.get(0).getPath();
            var blogSlug = segments.get(1).getPath();
            if (!isReservedSegment(username) && !isReservedSegment(blogSlug)) {
                return PageType.BLOG;
            }
        }

        if (segments.size() >= 3 && "page".equals(segments.get(1).getPath())) {
            var username = segments.get(0).getPath();
            if (!isReservedSegment(username)) {
                return PageType.USER;
            }
        }

        if (segments.size() >= 2 && "page".equals(segments.get(0).getPath())) {
            return PageType.GLOBAL;
        }

        return PageType.NONE;
    }

    public static String pathSlug(String storedSlug) {
        if (storedSlug == null || storedSlug.isBlank()) {
            return storedSlug;
        }
        return storedSlug.startsWith("/") ? storedSlug.substring(1) : storedSlug;
    }

    public static String publicUrl(CustomPage page) {
        var slug = pathSlug(page.getSlug());
        var blog = page.getBlog();
        if (blog == null) {
            return "/page/%s".formatted(slug);
        }
        var owner = blog.getOwner();
        if (blog.isMain()) {
            return "/%s/page/%s".formatted(owner.getUsername(), slug);
        }
        return "/%s/%s/page/%s".formatted(owner.getUsername(), blog.getSlug(), slug);
    }

    public static Set<String> reservedSegments() {
        return RESERVED_SEGMENTS;
    }

    public static String slug(java.util.List<jakarta.ws.rs.core.PathSegment> segments, PageType type) {
        return switch (type) {
            case GLOBAL -> segments.get(1).getPath();
            case USER -> segments.get(2).getPath();
            case BLOG -> segments.get(3).getPath();
            default -> throw new IllegalArgumentException("Unsupported page type: %s".formatted(type));
        };
    }

    public static String storedSlug(String pathSlug) {
        if (pathSlug == null || pathSlug.isBlank()) {
            return pathSlug;
        }
        return pathSlug.startsWith("/") ? pathSlug : "/%s".formatted(pathSlug);
    }

    public static String username(java.util.List<jakarta.ws.rs.core.PathSegment> segments) {
        return segments.get(0).getPath();
    }

    private CustomPagePaths() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
