package dev.vepo.contraponto.shared;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * URL slug rules for posts, tags, and series: lowercase letters, digits, and
 * hyphens only. Accented Latin letters are transliterated to canonical ASCII
 * (e.g. {@code ã} → {@code a}, {@code ç} → {@code c}) before slugifying. Blog
 * and custom-page slugs allow underscores and use separate validation.
 */
public final class Slug {

    private static final Pattern INVALID_SLUG_CHARS = Pattern.compile("[^a-z0-9\\-]");
    private static final Pattern SLUGIFY = Pattern.compile("[^a-z0-9]+|-{2,}");
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

    public static boolean hasInvalidSlugCharacters(String slug) {
        return slug != null && INVALID_SLUG_CHARS.matcher(slug).find();
    }

    public static String slugify(String raw) {
        if (raw == null) {
            return "";
        }
        String s = toCanonicalAscii(raw.toLowerCase(Locale.ROOT).trim());
        s = SLUGIFY.matcher(s).replaceAll("-");
        s = stripEdgeHyphens(s);
        if (s.length() > 255) {
            s = stripEdgeHyphens(s.substring(0, 255));
        }
        return s;
    }

    private static String stripEdgeHyphens(String s) {
        int start = 0;
        int end = s.length();
        while (start < end && s.charAt(start) == '-') {
            start++;
        }
        while (end > start && s.charAt(end - 1) == '-') {
            end--;
        }
        return s.substring(start, end);
    }

    private static String toCanonicalAscii(String s) {
        String normalized = Normalizer.normalize(s, Normalizer.Form.NFD);
        return DIACRITICS.matcher(normalized).replaceAll("");
    }

    private Slug() {}
}
