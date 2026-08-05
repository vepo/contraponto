package dev.vepo.contraponto.git.legacylink;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Author-supplied template describing the URL shape of a blog's pre-migration
 * self-links, e.g. {@code blog.vepo.dev/posts/{slug}}. Scheme-agnostic and
 * trailing-slash tolerant on the matched URL; the {@code {slug}} segment must
 * not itself contain a slash.
 */
public final class LegacyLinkPattern {

    private static final String SLUG_TOKEN = "{slug}";
    private static final Pattern SCHEME_PREFIX = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*://");

    public static Optional<LegacyLinkPattern> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        int firstIndex = trimmed.indexOf(SLUG_TOKEN);
        if (firstIndex < 0 || trimmed.indexOf(SLUG_TOKEN, firstIndex + SLUG_TOKEN.length()) >= 0) {
            return Optional.empty();
        }
        String withoutScheme = stripScheme(trimmed);
        if (!withoutScheme.contains("/")) {
            return Optional.empty();
        }
        int idx = withoutScheme.indexOf(SLUG_TOKEN);
        String prefix = withoutScheme.substring(0, idx);
        String suffix = withoutScheme.substring(idx + SLUG_TOKEN.length());
        if (prefix.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new LegacyLinkPattern(trimmed, prefix, suffix));
    }

    private static String stripScheme(String value) {
        return SCHEME_PREFIX.matcher(value).replaceFirst("");
    }

    private static String stripTrailingSlash(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '/') {
            end--;
        }
        return value.substring(0, end);
    }

    private final String raw;
    private final String prefix;
    private final String suffix;

    private LegacyLinkPattern(String raw, String prefix, String suffix) {
        this.raw = raw;
        this.prefix = prefix;
        this.suffix = suffix;
    }

    public Optional<String> extractSlug(String url) {
        if (url == null) {
            return Optional.empty();
        }
        String normalized = stripTrailingSlash(stripScheme(url.trim()));
        if (!normalized.startsWith(prefix) || !normalized.endsWith(suffix)) {
            return Optional.empty();
        }
        String slug = normalized.substring(prefix.length(), normalized.length() - suffix.length());
        if (slug.isEmpty() || slug.contains("/")) {
            return Optional.empty();
        }
        return Optional.of(slug);
    }

    public boolean matches(String url) {
        return extractSlug(url).isPresent();
    }

    public String raw() {
        return raw;
    }
}
