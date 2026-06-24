package dev.vepo.contraponto.post;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.blog.BlogAccess;
import dev.vepo.contraponto.shared.UnitTest;
import dev.vepo.contraponto.user.LoggedUser;
import dev.vepo.contraponto.user.Role;
import dev.vepo.contraponto.user.User;

@UnitTest
class PostAccessTest {

    private static Blog blog(long id, User owner) {
        var blog = new Blog();
        blog.setId(id);
        blog.setOwner(owner);
        blog.setMain(true);
        blog.setActive(true);
        blog.setName("Test");
        blog.setSlug("test");
        return blog;
    }

    private static Post post(long id, Blog blog) {
        var post = new Post();
        post.setId(id);
        post.setBlog(blog);
        post.setTitle("Title");
        post.setSlug("slug");
        post.setFormat(dev.vepo.contraponto.renderer.Format.MARKDOWN);
        return post;
    }

    private static User user(long id, String username, Role... roles) {
        var user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setName(username);
        user.setEmail("%s@example.com".formatted(username));
        user.setPasswordHash("hash");
        for (Role role : roles) {
            user.addRole(role);
        }
        return user;
    }

    private final PostAccess postAccess = new PostAccess(new BlogAccess());

    @Test
    void editorCannotManageOthersPost() {
        var owner = user(1L, "alice");
        var editor = user(2L, "editor", Role.EDITOR);
        var post = post(10L, blog(100L, owner));

        assertThat(postAccess.canManage(post, new LoggedUser(editor, "s"))).isFalse();
    }

    @Test
    void guestCannotManagePost() {
        var owner = user(1L, "alice");
        var post = post(10L, blog(100L, owner));

        assertThat(postAccess.canManage(post, new LoggedUser())).isFalse();
    }

    @Test
    void otherUserCannotManagePost() {
        var owner = user(1L, "alice");
        var other = user(2L, "bob");
        var post = post(10L, blog(100L, owner));

        assertThat(postAccess.canManage(post, new LoggedUser(other, "s"))).isFalse();
    }

    @Test
    void ownerCanManageOwnPost() {
        var owner = user(1L, "alice");
        var post = post(10L, blog(100L, owner));

        assertThat(postAccess.canManage(post, new LoggedUser(owner, "s"))).isTrue();
    }
}
