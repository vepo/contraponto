package dev.vepo.contraponto.post;

public final class PostPaths {

    public static String extractUrl(Post post) {
        var blog = post.getBlog();
        if (post.getBlog().isMain()) {
            return "/%s/post/%s".formatted(blog.getOwner().getUsername(), post.getSlug());
        }
        return "/%s/%s/post/%s".formatted(blog.getOwner().getUsername(), blog.getSlug(), post.getSlug());
    }

    private PostPaths() {}
}
