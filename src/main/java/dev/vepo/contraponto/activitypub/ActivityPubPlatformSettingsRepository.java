package dev.vepo.contraponto.activitypub;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ActivityPubPlatformSettingsRepository {

    private static final int SINGLETON_ID = 1;

    private final EntityManager entityManager;

    @Inject
    public ActivityPubPlatformSettingsRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public boolean isFederationEnabled() {
        var settings = entityManager.find(ActivityPubPlatformSettings.class, SINGLETON_ID);
        if (settings == null) {
            return true;
        }
        return settings.isFederationEnabled();
    }

    @Transactional
    public void setFederationEnabled(boolean federationEnabled) {
        var settings = entityManager.find(ActivityPubPlatformSettings.class, SINGLETON_ID);
        if (settings == null) {
            entityManager.persist(new ActivityPubPlatformSettings(SINGLETON_ID, federationEnabled));
            return;
        }
        settings.setFederationEnabled(federationEnabled);
        entityManager.merge(settings);
    }
}
