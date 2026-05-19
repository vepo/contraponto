package dev.vepo.contraponto.post;

/**
 * Published post snapshots store descriptions in {@code VARCHAR(512)}; the
 * working copy ({@link Post}) may hold a longer excerpt.
 */
public final class PostPublicationDescriptions {

    public static final int MAX_LENGTH = 512;

    public static boolean exceedsPublicationLimit(String raw) {
        if (raw == null) {
            return false;
        }
        return raw.stripTrailing().length() > MAX_LENGTH;
    }

    public static String truncateForPublication(String raw) {
        if (raw == null) {
            return "";
        }
        String stripped = raw.stripTrailing();
        if (stripped.length() <= MAX_LENGTH) {
            return stripped;
        }
        return stripped.substring(0, MAX_LENGTH);
    }

    private PostPublicationDescriptions() {}
}
