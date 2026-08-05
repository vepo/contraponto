package dev.vepo.contraponto.reddit;

import java.util.Optional;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.Startup;

import dev.vepo.contraponto.reddit.admin.RedditPlatformSettingsRepository;

@Startup
@ApplicationScoped
public class RedditSettings {

    private final boolean configEnabled;
    private final Optional<String> clientId;
    private final Optional<String> clientSecret;
    private final Optional<String> redirectUri;
    private final String userAgent;
    private final Optional<String> tokenEncryptionSecret;
    private final RedditPlatformSettingsRepository platformSettingsRepository;
    private volatile boolean platformSyndicationEnabled = true;

    @Inject
    public RedditSettings(@ConfigProperty(name = "contraponto.reddit.enabled", defaultValue = "false") boolean enabled,
                          @ConfigProperty(name = "contraponto.reddit.client-id") Optional<String> clientId,
                          @ConfigProperty(name = "contraponto.reddit.client-secret") Optional<String> clientSecret,
                          @ConfigProperty(name = "contraponto.reddit.redirect-uri") Optional<String> redirectUri,
                          @ConfigProperty(name = "contraponto.reddit.user-agent", defaultValue = "web:dev.vepo.contraponto:v1 (by /u/contraponto)") String userAgent,
                          @ConfigProperty(name = "contraponto.reddit.token-encryption-secret") Optional<String> tokenEncryptionSecret,
                          RedditPlatformSettingsRepository platformSettingsRepository) {
        this.configEnabled = enabled;
        this.clientId = clientId.map(String::trim).filter(s -> !s.isBlank());
        this.clientSecret = clientSecret.map(String::trim).filter(s -> !s.isBlank());
        this.redirectUri = redirectUri.map(String::trim).filter(s -> !s.isBlank());
        this.userAgent = userAgent;
        this.tokenEncryptionSecret = tokenEncryptionSecret.map(String::trim).filter(s -> !s.isBlank());
        this.platformSettingsRepository = platformSettingsRepository;
    }

    public Optional<String> clientId() {
        return clientId;
    }

    public Optional<String> clientSecret() {
        return clientSecret;
    }

    public boolean configEnabled() {
        return configEnabled;
    }

    public boolean enabled() {
        return configEnabled && platformSyndicationEnabled && clientId.isPresent() && clientSecret.isPresent()
                && redirectUri.isPresent();
    }

    @PostConstruct
    void loadPlatformSettings() {
        this.platformSyndicationEnabled = platformSettingsRepository.isSyndicationEnabled();
    }

    public boolean platformSyndicationEnabled() {
        return platformSyndicationEnabled;
    }

    public Optional<String> redirectUri() {
        return redirectUri;
    }

    public void setPlatformSyndicationEnabled(boolean enabled) {
        platformSettingsRepository.setSyndicationEnabled(enabled);
        this.platformSyndicationEnabled = enabled;
    }

    public Optional<String> tokenEncryptionSecret() {
        return tokenEncryptionSecret;
    }

    public String userAgent() {
        return userAgent;
    }
}
