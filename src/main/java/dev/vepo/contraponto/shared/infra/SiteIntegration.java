package dev.vepo.contraponto.shared.infra;

import java.net.URI;
import java.util.Optional;

import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Optional white-label third-party script injected in
 * {@code components/head.html}. Set both URL and data token at deploy time via
 * {@code APP_SITE_INTEGRATION_SCRIPT_URL} and
 * {@code APP_SITE_INTEGRATION_SCRIPT_DATA_TOKEN}.
 */
@ApplicationScoped
@Unremovable
public class SiteIntegration {

    static Optional<String> resolveDataToken(Optional<String> raw) {
        if (raw.isEmpty()) {
            return Optional.empty();
        }
        String trimmed = raw.get().trim();
        return trimmed.isEmpty() ? Optional.empty() : Optional.of(trimmed);
    }

    static Optional<String> resolveScriptUrl(Optional<String> raw) {
        if (raw.isEmpty()) {
            return Optional.empty();
        }
        String trimmed = raw.get().trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        try {
            URI uri = URI.create(trimmed);
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                return Optional.empty();
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                return Optional.empty();
            }
            return Optional.of(trimmed);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private final Optional<String> scriptUrl;

    private final Optional<String> scriptDataToken;

    @Inject
    public SiteIntegration(@ConfigProperty(name = "app.site.integration.script-url") Optional<String> scriptUrl,
                           @ConfigProperty(name = "app.site.integration.script-data-token") Optional<String> scriptDataToken) {
        this.scriptUrl = resolveScriptUrl(scriptUrl);
        this.scriptDataToken = resolveDataToken(scriptDataToken);
    }

    public boolean enabled() {
        return scriptUrl.isPresent() && scriptDataToken.isPresent();
    }

    public String scriptDataToken() {
        return scriptDataToken.orElse("");
    }

    public String scriptUrl() {
        return scriptUrl.orElse("");
    }
}
