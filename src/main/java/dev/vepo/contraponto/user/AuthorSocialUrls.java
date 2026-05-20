package dev.vepo.contraponto.user;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AuthorSocialUrls {

    private static void addIfValid(List<String> urls, String raw) {
        normalizeHttpsUrl(raw).filter(AuthorSocialUrls::isUrl).ifPresent(urls::add);
    }

    private static void addLink(List<SocialLink> links, String key, String label, String raw) {
        normalizeHttpsUrl(raw).filter(AuthorSocialUrls::isUrl).ifPresent(url -> links.add(new SocialLink(label, url, key)));
    }

    private static boolean isUrl(String value) {
        return value.startsWith("https://");
    }

    /**
     * @return normalized https URL, or empty if {@code raw} is blank, or a present
     *         error message if invalid
     */
    public static Optional<String> normalizeHttpsUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String trimmed = raw.trim();
        try {
            URI uri = URI.create(trimmed);
            if (uri.getScheme() == null || !"https".equalsIgnoreCase(uri.getScheme())) {
                return Optional.of("URL must use https.");
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                return Optional.of("URL is invalid.");
            }
            return Optional.of(trimmed);
        } catch (IllegalArgumentException e) {
            return Optional.of("URL is invalid.");
        }
    }

    public static List<String> sameAs(User user) {
        List<String> urls = new ArrayList<>();
        addIfValid(urls, user.getWebsiteUrl());
        addIfValid(urls, user.getTwitterUrl());
        addIfValid(urls, user.getMastodonUrl());
        addIfValid(urls, user.getGithubUrl());
        addIfValid(urls, user.getLinkedinUrl());
        return urls;
    }

    public static Optional<String> validateOptionalUrl(String raw) {
        return normalizeHttpsUrl(raw).filter(value -> value.startsWith("http"));
    }

    public static List<SocialLink> visibleLinks(User user) {
        List<SocialLink> links = new ArrayList<>();
        addLink(links, "website", "Site", user.getWebsiteUrl());
        addLink(links, "twitter", "X", user.getTwitterUrl());
        addLink(links, "mastodon", "Mastodon", user.getMastodonUrl());
        addLink(links, "github", "GitHub", user.getGithubUrl());
        addLink(links, "linkedin", "LinkedIn", user.getLinkedinUrl());
        return links;
    }

    private AuthorSocialUrls() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public record SocialLink(String label, String url, String key) {}
}
