package dev.vepo.contraponto.tag;

import java.util.Locale;
import java.util.regex.Pattern;

public final class TagSlug {

    private static final Pattern SLUG_GENERATION_PATTERN = Pattern.compile("[^a-zA-Z0-9\\-]");
    private static final Pattern INVALID_SLUG_CHARS = Pattern.compile("[^a-z0-9\\-]");

    public static boolean hasInvalidSlugCharacters(String slug) {
        return slug != null && INVALID_SLUG_CHARS.matcher(slug).find();
    }

    public static String slugify(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.toLowerCase(Locale.ROOT)
                      .trim()
                      .replaceAll(SLUG_GENERATION_PATTERN.pattern(), "-");
        s = s.replaceAll("-+", "-");
        s = s.replaceAll("^-+", "");
        s = s.replaceAll("-+$", "");
        if (s.length() > 255) {
            s = s.substring(0, 255);
            s = s.replaceAll("-+$", "");
        }
        return s;
    }

    private TagSlug() {}
}
