package dev.vepo.contraponto.rss;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.serie.Serie;
import dev.vepo.contraponto.tag.Tag;
import dev.vepo.contraponto.user.User;

class RssFeedPathsTest {

    @Test
    void buildsFeedPaths() {
        assertThat(RssFeedPaths.siteFeed()).isEqualTo("/feed");

        User owner = new User();
        owner.setUsername("alice");

        Blog main = new Blog();
        main.setMain(true);
        main.setOwner(owner);
        main.setSlug("main");
        assertThat(RssFeedPaths.blogFeed(main)).isEqualTo("/alice/feed/main-blog");

        Blog secondary = new Blog();
        secondary.setMain(false);
        secondary.setOwner(owner);
        secondary.setSlug("notes");
        assertThat(RssFeedPaths.blogFeed(secondary)).isEqualTo("/alice/notes/feed");

        Serie serie = new Serie();
        serie.setSlug("my-serie");
        serie.setBlog(main);
        assertThat(RssFeedPaths.serieFeed(serie)).isEqualTo("/alice/serie/my-serie/feed");

        Tag tag = new Tag();
        tag.setSlug("java");
        assertThat(RssFeedPaths.tagFeed(tag)).isEqualTo("/tags/java/feed");
    }
}
