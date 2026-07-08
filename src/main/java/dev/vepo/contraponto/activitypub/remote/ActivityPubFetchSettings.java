package dev.vepo.contraponto.activitypub.remote;

import java.time.Duration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ActivityPubFetchSettings {

    private final Duration connectTimeout;
    private final Duration requestTimeout;
    private final int maxPerDomainPerMinute;
    private final int profileMaxAgeDays;

    @Inject
    public ActivityPubFetchSettings(@ConfigProperty(name = "contraponto.activitypub.fetch.connect-timeout", defaultValue = "5s") Duration connectTimeout,
                                    @ConfigProperty(name = "contraponto.activitypub.fetch.request-timeout", defaultValue = "10s") Duration requestTimeout,
                                    @ConfigProperty(name = "contraponto.activitypub.fetch.max-per-domain-per-minute", defaultValue = "30") int maxPerDomainPerMinute,
                                    @ConfigProperty(name = "contraponto.activitypub.fetch.profile-max-age-days", defaultValue = "7") int profileMaxAgeDays) {
        this.connectTimeout = connectTimeout;
        this.requestTimeout = requestTimeout;
        this.maxPerDomainPerMinute = maxPerDomainPerMinute;
        this.profileMaxAgeDays = profileMaxAgeDays;
    }

    public Duration connectTimeout() {
        return connectTimeout;
    }

    public int maxPerDomainPerMinute() {
        return maxPerDomainPerMinute;
    }

    public int profileMaxAgeDays() {
        return profileMaxAgeDays;
    }

    public Duration requestTimeout() {
        return requestTimeout;
    }
}
