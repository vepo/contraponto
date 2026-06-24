package dev.vepo.contraponto.rss;

import dev.vepo.contraponto.shared.UnitTest;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostPublication;
import dev.vepo.contraponto.user.User;

@UnitTest
class RssFeedRendererTest {

    private static final DateTimeFormatter RFC1123 =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.ENGLISH).withZone(ZoneOffset.UTC);

    private static RssFeedRenderer.Channel channel() {
        return new RssFeedRenderer.Channel("Test feed", "https://example.com/alice", "Description");
    }

    private static Post postWithLivePublication(String slug, String title, String description, LocalDateTime publishedAt) {
        User owner = new User();
        owner.setUsername("alice");
        Blog blog = new Blog(owner);
        Post post = new Post();
        post.setBlog(blog);
        post.setSlug(slug);
        post.setTitle(title);
        post.setCreatedAt(LocalDateTime.of(2023, 1, 1, 0, 0));

        PostPublication live = new PostPublication();
        live.setTitle(title);
        live.setDescription(description);
        live.setPublishedAt(publishedAt);
        post.setLivePublication(live);
        return post;
    }

    @Test
    void escapeXmlEscapesSpecialCharacters() {
        assertThat(RssFeedRenderer.escapeXml("a & b <c> \"d\"")).isEqualTo("a &amp; b &lt;c&gt; &quot;d&quot;");
    }

    @Test
    void escapeXmlNullAndEmpty() {
        assertThat(RssFeedRenderer.escapeXml(null)).isEmpty();
        assertThat(RssFeedRenderer.escapeXml("")).isEmpty();
    }

    @Test
    void itemUriUsesAbsoluteLink() {
        assertThat(RssFeedRenderer.itemUri("https://alice.example/post/my-post").toString())
                                                                                            .isEqualTo("https://alice.example/post/my-post");
    }

    @Test
    void newestTimestampPicksLatestPublication() {
        Post older = postWithLivePublication("older", "Older", "Summary", LocalDateTime.of(2024, 1, 1, 10, 0));
        Post newer = postWithLivePublication("newer", "Newer", "Summary", LocalDateTime.of(2024, 6, 1, 10, 0));
        String xml = RssFeedRenderer.render(channel(), List.of(older, newer), post -> "https://example.com/alice/post/" + post.getSlug());

        String expectedLastBuild = RFC1123.format(ZonedDateTime.of(2024, 6, 1, 10, 0, 0, 0, ZoneOffset.UTC));
        assertThat(xml).contains("<lastBuildDate>" + expectedLastBuild + "</lastBuildDate>");
    }

    @Test
    void renderUsesTitleWhenSummaryBlank() {
        Post post = postWithLivePublication("my-post", "Post Title", "", LocalDateTime.of(2024, 3, 1, 12, 0));
        String xml = RssFeedRenderer.render(channel(), List.of(post), ignored -> "https://example.com/alice/post/my-post");

        assertThat(xml).contains("<description>Post Title</description>");
    }

    @Test
    void renderWithEmptyPostList() {
        String xml = RssFeedRenderer.render(channel(), List.of(), ignored -> "https://example.com/unused");

        assertThat(xml).contains("<rss version=\"2.0\">").contains("<lastBuildDate>");
    }
}
