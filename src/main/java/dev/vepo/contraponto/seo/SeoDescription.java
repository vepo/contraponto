package dev.vepo.contraponto.seo;

public final class SeoDescription {

    private static final int MAX_LENGTH = 160;

    public static String toPlainText(String markdownOrText) {
        if (markdownOrText == null || markdownOrText.isBlank()) {
            return "";
        }
        var plain = markdownOrText
                                  .replaceAll("<!--[\\s\\S]*?-->", " ")
                                  .replaceAll("<[^>]+>", " ")
                                  .replaceAll("[#*_>`\\[\\]()~]", " ")
                                  .replaceAll("\\s+", " ")
                                  .trim();
        return truncate(plain);
    }

    public static String truncate(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        if (text.length() <= MAX_LENGTH) {
            return text;
        }
        return "%s…".formatted(text.substring(0, MAX_LENGTH - 1).trim());
    }

    private SeoDescription() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
