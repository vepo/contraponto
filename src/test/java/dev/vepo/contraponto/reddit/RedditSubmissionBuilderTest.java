package dev.vepo.contraponto.reddit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.blog.BlogSubdomainConfig;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.shared.UnitTest;
import dev.vepo.contraponto.user.User;

/**
 * {@link RedditSubmissionBuilder} is a pure function — built entirely from
 * plain in-memory entities (no persistence), since a persisted {@code Post}
 * title is capped at 255 chars by the schema and can never exceed Reddit's
 * 300-char limit in practice; the truncation branch still needs a direct unit
 * test to guard against a future schema change.
 */
@UnitTest
class RedditSubmissionBuilderTest {

    private static BlogSubdomainConfig platformOnlyConfig() {
        return new BlogSubdomainConfig(false, Optional.empty(), Optional.empty(), "http://localhost:8080");
    }

    private static Post postWithTitle(String title) {
        var user = new User();
        user.setUsername("coolredditor");
        var blog = new Blog(user);
        var post = new Post();
        post.setBlog(blog);
        post.setSlug("some-post");
        post.setTitle(title);
        return post;
    }

    @Test
    void subredditIsOwnProfile() {
        var request = RedditSubmissionBuilder.build(postWithTitle("A short title"), "coolredditor", platformOnlyConfig());

        assertThat(request.subreddit()).isEqualTo("u_coolredditor");
        assertThat(request.url()).endsWith("/post/some-post");
    }

    @Test
    void titleLongerThan300CharsIsTruncated() {
        var request = RedditSubmissionBuilder.build(postWithTitle("T".repeat(320)), "coolredditor", platformOnlyConfig());

        assertThat(request.title()).hasSize(RedditSubmissionBuilder.MAX_TITLE_LENGTH);
    }

    @Test
    void titleWithin300CharsIsUnchanged() {
        var request = RedditSubmissionBuilder.build(postWithTitle("A perfectly normal title"),
                                                    "coolredditor",
                                                    platformOnlyConfig());

        assertThat(request.title()).isEqualTo("A perfectly normal title");
    }
}
