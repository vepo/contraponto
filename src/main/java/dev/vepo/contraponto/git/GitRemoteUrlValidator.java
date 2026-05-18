package dev.vepo.contraponto.git;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Validates blog git remote URLs to block SSRF against internal networks.
 */
@ApplicationScoped
public class GitRemoteUrlValidator {

    private static boolean isBlockedHost(String host) {
        String lower = host.toLowerCase(Locale.ROOT);
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
        } catch (UnknownHostException e) {
            return true;
        }
        return false;
    }

    private static boolean isCarrierGradeNat(InetAddress address) {
        byte[] octets = address.getAddress();
        if (octets.length != 4) {
            return false;
        }
        int first = octets[0] & 0xFF;
        int second = octets[1] & 0xFF;
        return first == 100 && second >= 64 && second <= 127;
    }

    public Optional<String> validate(String remoteUrl) {
        if (remoteUrl == null || remoteUrl.isBlank()) {
            return Optional.empty();
        }
        String trimmed = remoteUrl.trim();
        URI uri;
        try {
            uri = URI.create(trimmed);
        } catch (IllegalArgumentException e) {
            return Optional.of("Git remote URL is not valid.");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !scheme.equalsIgnoreCase("https")) {
            return Optional.of("Git remote URL must use HTTPS.");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            return Optional.of("Git remote URL must include a host name.");
        }
        if (isBlockedHost(host)) {
            return Optional.of("Git remote URL host is not allowed.");
        }
        return Optional.empty();
    }
}
