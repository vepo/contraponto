package dev.vepo.contraponto.notification;

import dev.vepo.contraponto.shared.UnitTest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.user.User;

@UnitTest
class BlogAudienceTest {

    @Test
    void equalsRequiresPersistedIds() {
        var first = new BlogAudience();
        setId(first, 1L);
        var second = new BlogAudience();
        setId(second, 1L);
        var unsaved = new BlogAudience();

        assertThat(first).isEqualTo(second);
        assertThat(first).isNotEqualTo(unsaved);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void isActiveWhenFollowedOrEmailSubscribed() {
        var user = new User();
        var blog = new Blog();
        var followOnly = new BlogAudience(user, blog, true, false);
        var emailOnly = new BlogAudience(user, blog, false, true);
        var inactive = new BlogAudience(user, blog, false, false);

        assertThat(followOnly.isActive()).isTrue();
        assertThat(emailOnly.isActive()).isTrue();
        assertThat(inactive.isActive()).isFalse();
    }

    @Test
    void onCreateSetsTimestampsWhenMissing() {
        var audience = new BlogAudience();
        audience.onCreate();
        assertThat(audience.getCreatedAt()).isNotNull();
        assertThat(audience.getUpdatedAt()).isEqualTo(audience.getCreatedAt());
    }

    @Test
    void onUpdateRefreshesUpdatedAt() {
        var audience = new BlogAudience();
        audience.onCreate();
        var before = audience.getUpdatedAt();
        audience.onUpdate();
        assertThat(audience.getUpdatedAt()).isAfterOrEqualTo(before);
    }

    private void setId(BlogAudience audience, long id) {
        try {
            var field = BlogAudience.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(audience, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
