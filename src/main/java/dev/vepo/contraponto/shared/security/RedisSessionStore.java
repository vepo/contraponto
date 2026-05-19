package dev.vepo.contraponto.shared.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.set.SetCommands;
import io.quarkus.redis.datasource.value.ValueCommands;

final class RedisSessionStore implements SessionStore {

    private static final Logger logger = LoggerFactory.getLogger(RedisSessionStore.class);
    private static final String SESSION_KEY_PREFIX = "session:";
    private static final String USER_SESSIONS_KEY_PREFIX = "user-sessions:";

    private static String sessionKey(String sessionId) {
        return SESSION_KEY_PREFIX + sessionId;
    }

    private static String userSessionsKey(long userId) {
        return USER_SESSIONS_KEY_PREFIX + userId;
    }

    private final ValueCommands<String, String> values;
    private final SetCommands<String, String> sets;

    private final KeyCommands<String> keys;

    private final long ttlSeconds;

    RedisSessionStore(RedisDataSource redis, long ttlSeconds) {
        this.values = redis.value(String.class);
        this.sets = redis.set(String.class);
        this.keys = redis.key();
        this.ttlSeconds = ttlSeconds;
    }

    @Override
    public Optional<Long> findUserId(String sessionId) {
        String userId = values.get(sessionKey(sessionId));
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(userId));
        } catch (NumberFormatException ex) {
            logger.warn("Invalid user id in session key {}: {}", sessionId, userId);
            remove(sessionId);
            return Optional.empty();
        }
    }

    @Override
    public void put(String sessionId, long userId) {
        String sessionKey = sessionKey(sessionId);
        values.setex(sessionKey, ttlSeconds, Long.toString(userId));
        String userSessionsKey = userSessionsKey(userId);
        sets.sadd(userSessionsKey, sessionId);
        keys.expire(userSessionsKey, ttlSeconds);
    }

    @Override
    public void remove(String sessionId) {
        findUserId(sessionId).ifPresent(userId -> {
            keys.del(sessionKey(sessionId));
            sets.srem(userSessionsKey(userId), sessionId);
        });
    }

    @Override
    public void removeAllForUser(long userId) {
        String userSessionsKey = userSessionsKey(userId);
        List<String> sessionIds = new ArrayList<>(sets.smembers(userSessionsKey));
        for (String sessionId : sessionIds) {
            keys.del(sessionKey(sessionId));
        }
        keys.del(userSessionsKey);
    }
}
