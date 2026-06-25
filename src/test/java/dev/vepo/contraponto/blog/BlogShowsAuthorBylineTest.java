package dev.vepo.contraponto.blog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.UnitTest;
import dev.vepo.contraponto.user.User;

@UnitTest
class BlogShowsAuthorBylineTest {

    @Test
    void hidesAuthorBylineWhenAuthorIsNull() {
        var author = new User();
        author.setName("Alice Smith");
        var blog = new Blog(author);

        assertThat(blog.showsAuthorByline(null)).isFalse();
    }

    @Test
    void hidesAuthorBylineWhenBlogNameMatchesDisplayName() {
        var author = new User();
        author.setName("Alice Smith");
        var blog = new Blog(author);

        assertThat(blog.showsAuthorByline(author)).isFalse();
    }

    @Test
    void showsAuthorBylineWhenBlogNameDiffersFromDisplayName() {
        var author = new User();
        author.setName("Alice Smith");
        var blog = new Blog(author);
        blog.setName("Tech Notes");

        assertThat(blog.showsAuthorByline(author)).isTrue();
    }
}
