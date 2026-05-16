package dev.vepo.contraponto.custompage;

import dev.vepo.contraponto.blog.Blog;

public final class CustomPagePaths {

    private CustomPagePaths() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static String pathSlug(String storedSlug) {
        if (storedSlug == null || storedSlug.isBlank()) {
            return storedSlug;
        }
        return storedSlug.startsWith("/") ? storedSlug.substring(1) : storedSlug;
    }

    public static String storedSlug(String pathSlug) {
        if (pathSlug == null || pathSlug.isBlank()) {
            return pathSlug;
        }
        return pathSlug.startsWith("/") ? pathSlug : "/" + pathSlug;
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
            default -> throw new IllegalArgumentException("Unsupported page type: " + type);
        }
        return builder.toString();
    }

    public static boolean isReservedSegment(String segment) {
        return segment.equals("components")
                || segment.equals("feed")
                || segment.equals("main-blog")
                || segment.equals("forms")
                || segment.equals("api")
                || segment.equals("write")
                || segment.equals("search")
                || segment.equals("library")
                || segment.equals("dashboard")
                || segment.equals("profile")
                || segment.equals("review")
                || segment.equals("pages")
                || segment.equals("blogs")
                || segment.equals("users")
                || segment.equals("post")
                || segment.equals("serie")
                || segment.equals("auth")
                || segment.equals("_custom_page");
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

    public static String username(java.util.List<jakarta.ws.rs.core.PathSegment> segments) {
        return segments.get(0).getPath();
    }

    public static String blogSlug(java.util.List<jakarta.ws.rs.core.PathSegment> segments) {
        return segments.get(1).getPath();
    }

    public static String slug(java.util.List<jakarta.ws.rs.core.PathSegment> segments, PageType type) {
        return switch (type) {
            case GLOBAL -> segments.get(1).getPath();
            case USER -> segments.get(2).getPath();
            case BLOG -> segments.get(3).getPath();
            default -> throw new IllegalArgumentException("Unsupported page type: " + type);
        };
    }

    public static long linksBlogId(CustomPage page) {
        var blog = page.getBlog();
        if (blog == null) {
            throw new IllegalStateException("Application pages do not have a blog");
        }
        return blog.getId();
    }

    public static boolean isMainBlogPage(CustomPage page) {
        Blog blog = page.getBlog();
        return blog != null && blog.isMain();
    }
}
