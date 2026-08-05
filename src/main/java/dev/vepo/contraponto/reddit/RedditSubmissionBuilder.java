package dev.vepo.contraponto.reddit;

import dev.vepo.contraponto.blog.BlogSubdomainConfig;
import dev.vepo.contraponto.post.Post;

/**
 * Pure builder for the Reddit link-post payload — no I/O, easy to unit test.
 */
public final class RedditSubmissionBuilder {

    /** Reddit rejects submission titles over 300 characters. */
    static final int MAX_TITLE_LENGTH = 300;

    public static RedditSubmissionRequest build(Post post, String redditUsername, BlogSubdomainConfig config) {
        var subreddit = RedditPaths.profileSubreddit(redditUsername);
        var title = truncateTitle(post.getTitle());
        var url = RedditPaths.canonicalPostUrl(post, config);
        return new RedditSubmissionRequest(subreddit, title, url);
    }

    private static String truncateTitle(String title) {
        if (title == null) {
            return "";
        }
        var trimmed = title.trim();
        if (trimmed.length() <= MAX_TITLE_LENGTH) {
            return trimmed;
        }
        return trimmed.substring(0, MAX_TITLE_LENGTH);
    }

    private RedditSubmissionBuilder() {}
}
