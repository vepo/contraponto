package dev.vepo.contraponto.notification;

import dev.vepo.contraponto.shared.UnitTest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.post.PostPublication;
import dev.vepo.contraponto.user.User;

@UnitTest
class EmailNotificationLogTest {

    @Test
    void constructorWiresPublicationAndUser() {
        var publication = new PostPublication();
        var user = new User();
        var log = new EmailNotificationLog(publication, user);
        assertThat(log.getPublication()).isSameAs(publication);
        assertThat(log.getUser()).isSameAs(user);
    }

    @Test
    void equalsRequiresPersistedIds() {
        var first = new EmailNotificationLog();
        setId(first, 1L);
        var second = new EmailNotificationLog();
        setId(second, 1L);
        var unsaved = new EmailNotificationLog();

        assertThat(first).isEqualTo(second);
        assertThat(first).isNotEqualTo(unsaved);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void onCreateSetsSentAtWhenMissing() {
        var log = new EmailNotificationLog(new PostPublication(), new User());
        log.onCreate();
        assertThat(log.getSentAt()).isNotNull();
    }

    private void setId(EmailNotificationLog log, long id) {
        try {
            var field = EmailNotificationLog.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(log, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
