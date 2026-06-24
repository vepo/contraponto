package dev.vepo.contraponto.blog;

import dev.vepo.contraponto.shared.UnitTest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.user.LoggedUser;
import dev.vepo.contraponto.user.Role;
import dev.vepo.contraponto.user.User;

@UnitTest
class BlogAccessTest {

    private static Blog blog(long id, User owner, boolean main) {
        var blog = new Blog();
        blog.setId(id);
        blog.setOwner(owner);
        blog.setMain(main);
        blog.setActive(true);
        blog.setName("Test");
        blog.setSlug("test");
        return blog;
    }

    private static User user(long id, String username, Role... roles) {
        var user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setName(username);
        user.setEmail(username + "@example.com");
        user.setPasswordHash("hash");
        for (Role role : roles) {
            user.addRole(role);
        }
        return user;
    }

    private final BlogAccess blogAccess = new BlogAccess();

    @Test
    void cannotDeactivateMainBlog() {
        var owner = user(1L, "alice");
        var editor = user(2L, "editor", Role.EDITOR);
        var blog = blog(10L, owner, true);

        assertThat(blogAccess.canDeactivate(blog, new LoggedUser(editor, "s"))).isFalse();
        assertThat(blogAccess.canDeactivate(blog, new LoggedUser(owner, "s"))).isFalse();
    }

    @Test
    void editorCanDeactivateOthersSecondaryBlog() {
        var owner = user(1L, "alice");
        var editor = user(2L, "editor", Role.EDITOR);
        var blog = blog(10L, owner, false);

        assertThat(blogAccess.canDeactivate(blog, new LoggedUser(editor, "s"))).isTrue();
        assertThat(blogAccess.canDeactivate(blog, new LoggedUser(owner, "s"))).isTrue();
    }

    @Test
    void ownerCanEditOwnBlog() {
        var owner = user(1L, "alice");
        var editor = user(2L, "editor", Role.EDITOR);
        var blog = blog(10L, owner, false);

        assertThat(blogAccess.canEdit(blog, new LoggedUser(owner, "s"))).isTrue();
        assertThat(blogAccess.canEdit(blog, new LoggedUser(editor, "s"))).isFalse();
    }
}
