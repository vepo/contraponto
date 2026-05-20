package dev.vepo.contraponto.view;

import dev.vepo.contraponto.shared.UnitTest;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.user.User;

@UnitTest
class ViewTest {

    @Test
    void parameterizedConstructorAndBeansMutationsRoundTrip() {
        LocalDateTime watched = LocalDateTime.of(2025, 1, 1, 13, 0);
        Post post = new Post();
        User watcher = new User();
        watcher.setUsername("spectator");

        View view = new View(post, watcher, "session-uuid", watched);
        assertThat(view.getPost()).isSameAs(post);
        assertThat(view.getUser()).isSameAs(watcher);
        assertThat(view.getSessionId()).isEqualTo("session-uuid");
        assertThat(view.getViewedAt()).isEqualTo(watched);

        view.setId(321L);
        assertThat(view.toString()).contains("321");

        view.setPost(new Post());
        view.setUser(null);
        view.setSessionId("anon-session");
        view.setViewedAt(watched.plusHours(3));
        assertThat(view.getUser()).isNull();
        assertThat(view.getSessionId()).isEqualTo("anon-session");
    }
}
