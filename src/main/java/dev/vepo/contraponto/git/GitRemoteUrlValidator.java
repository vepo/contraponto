package dev.vepo.contraponto.git;

import java.util.Optional;

import dev.vepo.contraponto.shared.security.OutboundHttpsUrlValidator;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Validates blog git remote URLs to block SSRF against internal networks.
 */
@ApplicationScoped
public class GitRemoteUrlValidator {

    private final OutboundHttpsUrlValidator outboundHttpsUrlValidator;

    @Inject
    public GitRemoteUrlValidator(OutboundHttpsUrlValidator outboundHttpsUrlValidator) {
        this.outboundHttpsUrlValidator = outboundHttpsUrlValidator;
    }

    private String mapGitMessage(String error) {
        if ("URL must use HTTPS.".equals(error)) {
            return "Git remote URL must use HTTPS.";
        }
        if ("URL must include a host name.".equals(error)) {
            return "Git remote URL must include a host name.";
        }
        if ("URL host is not allowed.".equals(error)) {
            return "Git remote URL host is not allowed.";
        }
        if ("URL is not valid.".equals(error)) {
            return "Git remote URL is not valid.";
        }
        return "Git remote URL is not valid.";
    }

    public Optional<String> validate(String remoteUrl) {
        if (remoteUrl == null || remoteUrl.isBlank()) {
            return Optional.empty();
        }
        var error = outboundHttpsUrlValidator.validateHttpsUrl(remoteUrl.trim());
        if (error.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapGitMessage(error.get()));
    }
}
