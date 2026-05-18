package dev.vepo.contraponto.shared.infra;

public final class AvatarSvgRenderer {

    private static final String BACKGROUND = "#1a8917";
    private static final String TEXT_COLOR = "#ffffff";

    private static String escapeXml(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
    }

    public static String render(String initials) {
        var safeInitials = initials == null ? "" : initials;
        if (safeInitials.length() > 2) {
            safeInitials = safeInitials.substring(0, 2);
        }
        return """
               <svg xmlns="http://www.w3.org/2000/svg" width="128" height="128" viewBox="0 0 128 128" role="img" aria-label="%s">
                 <rect width="128" height="128" fill="%s"/>
                 <text x="64" y="64" dominant-baseline="central" text-anchor="middle"
                       fill="%s" font-family="system-ui, -apple-system, sans-serif" font-size="48" font-weight="700">%s</text>
               </svg>
               """.formatted(escapeXml(safeInitials), BACKGROUND, TEXT_COLOR, escapeXml(safeInitials));
    }

    private AvatarSvgRenderer() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
