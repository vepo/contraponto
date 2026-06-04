package dev.vepo.contraponto.image;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class ImageControlUrls {

    public static final String HUB_PATH = "/writing/images";

    public static String extraQuery(String searchQuery) {
        if (searchQuery == null || searchQuery.isBlank()) {
            return "";
        }
        return "&q=" + URLEncoder.encode(searchQuery.trim(), StandardCharsets.UTF_8);
    }

    private ImageControlUrls() {
        throw new UnsupportedOperationException("Utility class");
    }
}
