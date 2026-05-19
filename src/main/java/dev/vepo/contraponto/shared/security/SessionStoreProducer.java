package dev.vepo.contraponto.shared.security;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.redis.datasource.RedisDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class SessionStoreProducer {

    @ConfigProperty(name = "app.session.store", defaultValue = "memory")
    String sessionStore;

    @ConfigProperty(name = "app.session.ttl-seconds", defaultValue = "2592000")
    long ttlSeconds;

    @Produces
    @ApplicationScoped
    SessionStore sessionStore(Instance<RedisDataSource> redisDataSource) {
        if ("redis".equalsIgnoreCase(sessionStore)) {
            return new RedisSessionStore(redisDataSource.get(), ttlSeconds);
        }
        return new InMemorySessionStore();
    }
}
