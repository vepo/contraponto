package dev.vepo.contraponto.reddit.admin;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class RedditPlatformSettingsRepository {

    private static final int SINGLETON_ID = 1;

    private final EntityManager entityManager;

    @Inject
    public RedditPlatformSettingsRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public boolean isSyndicationEnabled() {
        var settings = entityManager.find(RedditPlatformSettings.class, SINGLETON_ID);
        if (settings == null) {
            return true;
        }
        return settings.isSyndicationEnabled();
    }

    @Transactional
    public void setSyndicationEnabled(boolean syndicationEnabled) {
        var settings = entityManager.find(RedditPlatformSettings.class, SINGLETON_ID);
        if (settings == null) {
            entityManager.persist(new RedditPlatformSettings(SINGLETON_ID, syndicationEnabled));
            return;
        }
        settings.setSyndicationEnabled(syndicationEnabled);
        entityManager.merge(settings);
    }
}
