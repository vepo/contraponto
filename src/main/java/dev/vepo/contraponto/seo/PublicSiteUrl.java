package dev.vepo.contraponto.seo;

import dev.vepo.contraponto.image.Image;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class PublicSiteUrl {

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:8080";
        }
        var trimmed = url.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private final String baseUrl;

    public PublicSiteUrl(@ConfigProperty(name = "image.base.url", defaultValue = "http://localhost:8080") String baseUrl) {
        this.baseUrl = trimTrailingSlash(baseUrl);
    }

    public String absolute(Image image) {
        if (image == null || image.getUrl() == null || image.getUrl().isBlank()) {
            return null;
        }
        return absolute(image.getUrl());
    }

    public String absolute(String path) {
        if (path == null || path.isBlank()) {
            return baseUrl;
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        if (path.startsWith("/")) {
            return "%s%s".formatted(baseUrl, path);
        }
        return "%s/%s".formatted(baseUrl, path);
    }

    public String baseUrl() {
        return baseUrl;
    }
}
