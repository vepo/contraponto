package dev.vepo.contraponto.git;

import dev.vepo.contraponto.shared.UnitTest;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.renderer.Format;
import dev.vepo.contraponto.serie.Serie;
import dev.vepo.contraponto.tag.Tag;
import dev.vepo.contraponto.user.User;

@UnitTest
class BlogGitMarkdownMapperTest {

    private static Tag tag(String slug, String name) {
        return new Tag(slug, name, null);
    }

    @Test
    void buildFrontMatterIncludesSlugLayoutTagsAndPublishingFields() throws Exception {
        User owner = new User();
        Blog blog = new Blog(owner, "slug", "Name", "Desc");
        Post post = new Post();
        post.setId(42L);
        post.setSlug("hello-world");
        post.setTitle("Hello");
        post.setDescription("Short");
        post.setBlog(blog);
        post.setContent("ignored");
        post.setFormat(Format.ASCIIDOC);
        post.setFeatured(true);
        post.setPublished(true);
        LocalDateTime publishedAt = LocalDateTime.of(2024, 3, 1, 14, 30);
        post.setPublishedAt(publishedAt);
        post.getTags().add(tag("bee", "B"));
        post.getTags().add(tag("aa", "a"));
        Serie serie = new Serie(blog, "My Series", "my-series");
        post.setSerie(serie);

        JekyllLayoutConvention c = JekyllLayoutConvention.defaults();
        LinkedHashMap<String, Object> fm = BlogGitMarkdownMapper.buildFrontMatter(post, c);

        assertThat(fm).containsEntry(JekyllLayoutConvention.FM_POST_ID, 42L)
                      .containsEntry("slug", "hello-world")
                      .containsEntry(c.layoutFrontMatterKey(), c.defaultLayoutValue())
                      .containsEntry("title", "Hello")
                      .containsEntry("description", "Short")
                      .containsEntry("tags", List.of("a", "B"))
                      .containsEntry("serie", "My Series")
                      .containsEntry("featured", true)
                      .containsEntry("published", true)
                      .containsEntry("format", Format.ASCIIDOC.name());
        assertThat(fm.get("published_at")).isInstanceOf(String.class);
        assertThat((String) fm.get("published_at")).startsWith("2024-03-01");
        PostGitMarkdownCodec codec = new PostGitMarkdownCodec();
        String roundTrip = codec.writeMarkdownDocument(fm, "");
        assertThat(roundTrip).contains("hello-world").contains("contraponto_post_id");
    }

    @Test
    void buildFrontMatterOmitsPostIdWhenNullAndUsesEmptyDescription() {
        Blog blog = new Blog();
        Post post = new Post();
        post.setSlug("s");
        post.setTitle("T");
        post.setDescription(null);
        post.setBlog(blog);
        post.setFeatured(false);
        post.setPublished(false);
        post.setFormat(Format.MARKDOWN);

        LinkedHashMap<String, Object> fm = BlogGitMarkdownMapper.buildFrontMatter(post, JekyllLayoutConvention.defaults());

        assertThat(fm.containsKey(JekyllLayoutConvention.FM_POST_ID)).isFalse();
        assertThat(fm).containsEntry("description", "");
        assertThat(fm).doesNotContainKey("serie");
    }
}
