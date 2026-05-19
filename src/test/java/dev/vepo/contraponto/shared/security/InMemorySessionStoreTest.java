package dev.vepo.contraponto.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemorySessionStoreTest {

    private InMemorySessionStore sessionStore;

    @Test
    void removeAllForUserWithNoSessions() {
        sessionStore.removeAllForUser(999L);

        assertThat(sessionStore.findUserId("any")).isEmpty();
    }

    @Test
    void removeLastSessionCleansUserIndex() {
        sessionStore.put("session-a", 42L);

        sessionStore.remove("session-a");

        assertThat(sessionStore.findUserId("session-a")).isEmpty();
        sessionStore.removeAllForUser(42L);
        assertThat(sessionStore.findUserId("session-a")).isEmpty();
    }

    @Test
    void removeUnknownSessionIsNoOp() {
        sessionStore.remove("ghost");

        assertThat(sessionStore.findUserId("ghost")).isEmpty();
    }

    @BeforeEach
    void setUp() {
        sessionStore = new InMemorySessionStore();
    }

    @Test
    void shouldRemoveAllSessionsForUser() {
        sessionStore.put("session-a", 7L);
        sessionStore.put("session-b", 7L);
        sessionStore.put("session-c", 9L);

        sessionStore.removeAllForUser(7L);

        assertThat(sessionStore.findUserId("session-a")).isEmpty();
        assertThat(sessionStore.findUserId("session-b")).isEmpty();
        assertThat(sessionStore.findUserId("session-c")).contains(9L);
    }

    @Test
    void shouldRemoveSession() {
        sessionStore.put("session-a", 42L);

        sessionStore.remove("session-a");

        assertThat(sessionStore.findUserId("session-a")).isEmpty();
    }

    @Test
    void shouldStoreAndResolveSession() {
        sessionStore.put("session-a", 42L);

        assertThat(sessionStore.findUserId("session-a")).contains(42L);
    }

    @Test
    void unknownSessionReturnsEmpty() {
        assertThat(sessionStore.findUserId("missing")).isEmpty();
    }
}
