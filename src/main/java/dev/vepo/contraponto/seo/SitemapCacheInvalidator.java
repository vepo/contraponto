package dev.vepo.contraponto.seo;

import dev.vepo.contraponto.custompage.CustomPageChangedEvent;
import dev.vepo.contraponto.notification.PostPublishedEvent;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class SitemapCacheInvalidator {

    private static final String CACHE_NAME = "sitemap";

    private final Cache sitemapCache;

    @Inject
    public SitemapCacheInvalidator(CacheManager cacheManager) {
        this.sitemapCache = cacheManager.getCache(CACHE_NAME).orElseThrow();
    }

    void afterCustomPageChanged(@Observes CustomPageChangedEvent event) {
        invalidate();
    }

    void afterPostPublished(@Observes PostPublishedEvent event) {
        invalidate();
    }

    private void invalidate() {
        sitemapCache.invalidateAll().await().indefinitely();
    }
}
