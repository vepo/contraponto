package dev.vepo.contraponto.blog;

public final class BlogPaths {

    public static String extractUrl(Blog blog) {
        if (blog.isMain()) {
            return "/%s".formatted(blog.getOwner().getUsername());
        }
        return "/%s/%s".formatted(blog.getOwner().getUsername(), blog.getSlug());
    }

    private BlogPaths() {}
}
