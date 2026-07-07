package dev.vepo.contraponto.activitypub;

import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.annotation.PostConstruct;

@Startup
@ApplicationScoped
public class ActivityPubSettings {

    private final boolean configEnabled;
    private final Optional<String> keyEncryptionSecret;
    private final boolean insecureAcceptUnsigned;
    private final ActivityPubPlatformSettingsRepository platformSettingsRepository;
    private volatile boolean platformFederationEnabled = true;

    @Inject
    public ActivityPubSettings(@ConfigProperty(name = "contraponto.activitypub.enabled", defaultValue = "false") boolean enabled,
                               @ConfigProperty(name = "contraponto.activitypub.key-encryption-secret") Optional<String> keyEncryptionSecret,
                               @ConfigProperty(name = "contraponto.activitypub.insecure-accept-unsigned", defaultValue = "false") boolean insecureAcceptUnsigned,
                               ActivityPubPlatformSettingsRepository platformSettingsRepository) {
        this.configEnabled = enabled;
        this.keyEncryptionSecret = keyEncryptionSecret.map(String::trim).filter(s -> !s.isBlank());
        this.insecureAcceptUnsigned = insecureAcceptUnsigned;
        this.platformSettingsRepository = platformSettingsRepository;
    }

    public boolean configEnabled() {
        return configEnabled;
    }

    public boolean enabled() {
        return configEnabled && platformFederationEnabled;
    }

    public boolean insecureAcceptUnsigned() {
        return insecureAcceptUnsigned;
    }

    public Optional<String> keyEncryptionSecret() {
        return keyEncryptionSecret;
    }

    @PostConstruct
    void loadPlatformSettings() {
        this.platformFederationEnabled = platformSettingsRepository.isFederationEnabled();
    }

    public void setPlatformFederationEnabled(boolean enabled) {
        platformSettingsRepository.setFederationEnabled(enabled);
        this.platformFederationEnabled = enabled;
    }
}
