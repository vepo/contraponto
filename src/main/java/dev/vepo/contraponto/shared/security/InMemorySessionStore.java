package dev.vepo.contraponto.shared.security;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class InMemorySessionStore implements SessionStore {

    private final Map<String, Long> sessionToUser = Collections.synchronizedMap(new HashMap<>());
    private final Map<Long, Set<String>> userToSessions = Collections.synchronizedMap(new HashMap<>());

    @Override
    public Optional<Long> findUserId(String sessionId) {
        return Optional.ofNullable(sessionToUser.get(sessionId));
    }

    @Override
    public void put(String sessionId, long userId) {
        sessionToUser.put(sessionId, userId);
        userToSessions.computeIfAbsent(userId, ignored -> Collections.synchronizedSet(new HashSet<>()))
                      .add(sessionId);
    }

    @Override
    public void remove(String sessionId) {
        Long userId = sessionToUser.remove(sessionId);
        if (userId != null) {
            var sessions = userToSessions.get(userId);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    userToSessions.remove(userId);
                }
            }
        }
    }

    @Override
    public void removeAllForUser(long userId) {
        var sessions = userToSessions.remove(userId);
        if (sessions != null) {
            for (String sessionId : sessions) {
                sessionToUser.remove(sessionId);
            }
        }
    }
}
