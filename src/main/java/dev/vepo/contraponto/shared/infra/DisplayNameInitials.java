package dev.vepo.contraponto.shared.infra;

public final class DisplayNameInitials {

    public static String from(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return "";
        }
        var parts = displayName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    private DisplayNameInitials() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
