package dev.vepo.contraponto.reddit;

import dev.vepo.contraponto.blog.BlogSubdomainConfig;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostPaths;

public final class RedditPaths {

    /**
     * Canonical absolute post URL — same subdomain/platform fallback rule as
     * {@code ActivityPubPaths.postObjectId}.
     */
    public static String canonicalPostUrl(Post post, BlogSubdomainConfig config) {
        var blog = post.getBlog();
        var owner = blog.getOwner();
        if (config.enabled()) {
            var path = blog.isMain() ? "/post/%s".formatted(post.getSlug())
                                     : "/%s/post/%s".formatted(blog.getSlug(), post.getSlug());
            return "%s%s".formatted(config.subdomainOrigin(owner.getUsername()), path);
        }
        return config.platformUrl(PostPaths.extractUrl(post));
    }

    /** Reddit's own-profile pseudo-subreddit name for a link submission. */
    public static String profileSubreddit(String redditUsername) {
        return "u_%s".formatted(redditUsername);
    }

    private RedditPaths() {}
}
