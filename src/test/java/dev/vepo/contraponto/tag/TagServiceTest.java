package dev.vepo.contraponto.tag;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class TagServiceTest {

    @Inject
    TagService tagService;

    private User author;
    private Blog blog;
    private Post post;

    @BeforeEach
    void setUp() {
        Given.cleanup();
        author = Given.user()
                      .withUsername("tagauth")
                      .withEmail("tagauth@example.com")
                      .withPassword("Password123!")
                      .withName("Tag Author")
                      .persist();
        blog = author.getDefaultBlog();
        post = Given.post()
                    .withAuthor(author)
                    .withBlog(blog)
                    .withTitle("Tagged post")
                    .withSlug("tagged-post")
                    .withContent("Body")
                    .persist();
    }

    @Test
    void syncPostTagsClearsTagsWhenJsonBlank() {
        Given.transaction(() -> {
            tagService.syncPostTags(post, "[\"Java\"]");
            assertThat(post.getTags()).hasSize(1);

            tagService.syncPostTags(post, null);
            assertThat(post.getTags()).isEmpty();

            tagService.syncPostTags(post, "");
            assertThat(post.getTags()).isEmpty();

            tagService.syncPostTags(post, "   ");
            assertThat(post.getTags()).isEmpty();
        });
    }

    @Test
    void syncPostTagsIgnoresMalformedJson() {
        Given.transaction(() -> {
            tagService.syncPostTags(post, "not-json");
            assertThat(post.getTags()).isEmpty();
        });
    }

    @Test
    void syncPostTagsTrimsAndSkipsBlanks() {
        Given.transaction(() -> {
            tagService.syncPostTags(post, "[\"  Java  \", \"\", \"Java\"]");
            assertThat(post.getTags()).hasSize(1);
            assertThat(post.getTags().getFirst().getName()).isEqualTo("Java");
        });
    }

    @Test
    void tagsToJsonReturnsEmptyArrayWhenNoTags() {
        assertThat(tagService.tagsToJson(post)).isEqualTo("[]");
    }

    @Test
    void tagsToJsonSortsCaseInsensitively() {
        Given.transaction(() -> {
            tagService.syncPostTags(post, "[\"Zebra\", \"apple\"]");
            assertThat(tagService.tagsToJson(post)).isEqualTo("[\"apple\",\"Zebra\"]");
        });
    }
}
