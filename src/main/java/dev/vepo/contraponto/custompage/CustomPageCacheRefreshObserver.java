package dev.vepo.contraponto.custompage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class CustomPageCacheRefreshObserver {

    private final CustomPageCache customPageCache;

    @Inject
    public CustomPageCacheRefreshObserver(CustomPageCache customPageCache) {
        this.customPageCache = customPageCache;
    }

    void afterCustomPageChanged(@Observes CustomPageChangedEvent event) {
        customPageCache.refresh(event.pageId());
    }
}
