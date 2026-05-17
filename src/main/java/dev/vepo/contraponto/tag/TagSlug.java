package dev.vepo.contraponto.tag;

import java.util.Locale;
import java.util.regex.Pattern;

public final class TagSlug {

    private static final Pattern INVALID_SLUG_CHARS = Pattern.compile("[^a-z0-9\\-]");
    private static final Pattern SLUGIFY = Pattern.compile("[^a-z0-9]+|-{2,}");

    public static boolean hasInvalidSlugCharacters(String slug) {
        return slug != null && INVALID_SLUG_CHARS.matcher(slug).find();
    }

    public static String slugify(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.toLowerCase(Locale.ROOT).trim();
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

    private TagSlug() {}
}
