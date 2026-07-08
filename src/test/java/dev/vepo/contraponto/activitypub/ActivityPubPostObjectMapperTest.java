package dev.vepo.contraponto.activitypub;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.blog.BlogSubdomainConfig;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.user.User;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class ActivityPubPostObjectMapperTest {

    private static final LocalDateTime PUBLISHED_AT = LocalDateTime.of(2024, 3, 15, 10, 30, 0);

    private static User author() {
        var user = new User();
        user.setUsername("alice");
        user.setName("Alice");
        return user;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    private static Post mainBlogPost() {
        var owner = author();
        var blog = new Blog(owner);
        var post = new Post();
        post.setId(11L);
        post.setSlug("main-note");
        post.setTitle("Main Note");
        post.setDescription("Ignore this description");
        post.setContent("Body");
        post.setBlog(blog);
        post.setPublished(true);
        post.setPublishedAt(PUBLISHED_AT);
        return post;
    }

    private static Post secondaryBlogPost() {
        var owner = author();
        var blog = new Blog(owner, "lab-notes", "Lab Notes", "Experiments");
        var post = new Post();
        post.setId(22L);
        post.setSlug("secondary-note");
        post.setTitle("Secondary Note");
        post.setDescription("Secondary description ignored");
        post.setContent("Body");
        post.setBlog(blog);
        post.setPublished(true);
        post.setPublishedAt(PUBLISHED_AT);
        return post;
    }

    @Inject
    ActivityPubPostObjectMapper mapper;

    @Inject
    BlogSubdomainConfig subdomainConfig;

    @Test
    void createActivityForMainBlogUsesTitleAndLinkWithoutSummaryOrDescription() {
        var post = mainBlogPost();
        var expectedUrl = ActivityPubPaths.postObjectId(post, subdomainConfig);
        var activity = mapper.toCreateActivity(post);
        var object = castMap(activity.get("object"));

        assertThat(activity.get("type")).isEqualTo("Create");
        assertThat(activity.get("published")).isEqualTo("2024-03-15T10:30:00Z");
        assertThat(activity).doesNotContainKey("summary");

        assertThat(object.get("id")).isEqualTo(expectedUrl);
        assertThat(object.get("url")).isEqualTo(expectedUrl);
        assertThat(object.get("content")).isEqualTo("<p><strong>Main Note</strong></p><p><a href=\"%s\">%s</a></p>".formatted(expectedUrl,
                                                                                                                              expectedUrl));
        assertThat((String) object.get("content")).doesNotContain("Ignore this description");
        assertThat(object).doesNotContainKey("summary");
        assertThat(object.get("attributedTo")).isEqualTo(activity.get("actor"));
    }

    @Test
    void createActivityForSecondaryBlogIncludesBlogNameAndSecondaryPath() {
        var post = secondaryBlogPost();
        var expectedUrl = ActivityPubPaths.postObjectId(post, subdomainConfig);
        var activity = mapper.toCreateActivity(post);
        var object = castMap(activity.get("object"));

        assertThat(expectedUrl).contains("/alice/lab-notes/post/secondary-note");
        assertThat(activity.get("published")).isEqualTo("2024-03-15T10:30:00Z");
        assertThat(object.get("id")).isEqualTo(expectedUrl);
        assertThat(object.get("url")).isEqualTo(expectedUrl);
        assertThat(object.get("content")).isEqualTo(
                                                    "<p><strong>Secondary Note</strong></p><p>Lab Notes</p><p><a href=\"%s\">%s</a></p>".formatted(expectedUrl,
                                                                                                                                                   expectedUrl));
        assertThat((String) object.get("content")).doesNotContain("Secondary description ignored");
        assertThat(object).doesNotContainKey("summary");
        assertThat(object.get("attributedTo")).isEqualTo(activity.get("actor"));
    }
}
