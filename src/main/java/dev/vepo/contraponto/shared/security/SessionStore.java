package dev.vepo.contraponto.shared.security;

import java.util.Optional;

/**
 * Authenticated session backing store ({@code __session} cookie → user id).
 */
public interface SessionStore {

    Optional<Long> findUserId(String sessionId);

    void put(String sessionId, long userId);

    void remove(String sessionId);

    void removeAllForUser(long userId);
}
