package dev.vepo.contraponto.shared.security;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Validates outbound HTTPS URLs to block SSRF against internal networks.
 */
@ApplicationScoped
public class OutboundHttpsUrlValidator {

    private final Optional<List<String>> testAllowedHosts;

    @Inject
    public OutboundHttpsUrlValidator(@ConfigProperty(name = "contraponto.security.outbound-test-allowed-hosts") Optional<List<String>> testAllowedHosts) {
        this.testAllowedHosts = testAllowedHosts.map(hosts -> hosts.stream()
                                                                   .map(host -> host.toLowerCase(Locale.ROOT))
                                                                   .toList());
    }

    private boolean isBlockedHost(String host) {
        var lower = host.toLowerCase(Locale.ROOT);
        if (lower.equals("localhost") || lower.endsWith(".localhost")) {
            return true;
        }
        if (lower.endsWith(".local") || lower.endsWith(".internal")) {
            return true;
        }
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (address.isAnyLocalAddress()
                        || address.isLoopbackAddress()
                        || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress()
                        || isCarrierGradeNat(address)) {
                    return true;
                }
            }
        } catch (UnknownHostException ex) {
            return true;
        }
        return false;
    }

    private boolean isCarrierGradeNat(InetAddress address) {
        byte[] octets = address.getAddress();
        if (octets.length != 4) {
            return false;
        }
        int first = octets[0] & 0xFF;
        int second = octets[1] & 0xFF;
        return first == 100 && second >= 64 && second <= 127;
    }

    private boolean isHostAllowed(String host) {
        var lower = host.toLowerCase(Locale.ROOT);
        if (testAllowedHosts.isPresent() && testAllowedHosts.get().contains(lower)) {
            return true;
        }
        return !isBlockedHost(host);
    }

    public Optional<String> validateHttpsUrl(String url) {
        if (url == null || url.isBlank()) {
            return Optional.of("URL is required.");
        }
        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (IllegalArgumentException ex) {
            return Optional.of("URL is not valid.");
        }
        var scheme = uri.getScheme();
        if (scheme == null || !scheme.equalsIgnoreCase("https")) {
            return Optional.of("URL must use HTTPS.");
        }
        var host = uri.getHost();
        if (host == null || host.isBlank()) {
            return Optional.of("URL must include a host name.");
        }
        if (!isHostAllowed(host)) {
            return Optional.of("URL host is not allowed.");
        }
        return Optional.empty();
    }
}
