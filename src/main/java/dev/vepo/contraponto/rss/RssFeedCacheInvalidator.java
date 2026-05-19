package dev.vepo.contraponto.rss;

import dev.vepo.contraponto.notification.PostPublishedEvent;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class RssFeedCacheInvalidator {

    private static final String CACHE_NAME = "rss-feeds";

    private final Cache rssFeedCache;

    @Inject
    public RssFeedCacheInvalidator(CacheManager cacheManager) {
        this.rssFeedCache = cacheManager.getCache(CACHE_NAME).orElseThrow();
    }

    void afterPostPublished(@Observes PostPublishedEvent event) {
        rssFeedCache.invalidateAll().await().indefinitely();
    }
}
