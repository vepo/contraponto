package dev.vepo.contraponto.navigation;

public record HubSectionNav(String slug, String label, String i18nKey, String pathOverride) {

    public HubSectionNav(String slug, String label) {
        this(slug, label, null, null);
    }

    public HubSectionNav(String slug, String label, String i18nKey) {
        this(slug, label, i18nKey, null);
    }

    public String sectionPath(NavigationHub hub) {
        if (pathOverride != null && !pathOverride.isBlank()) {
            return pathOverride;
        }
        return "%s/%s".formatted(hub.path(), slug);
    }

    public String resolvedPath(String hubBasePath) {
        if (pathOverride != null && !pathOverride.isBlank()) {
            return pathOverride;
        }
        return "%s/%s".formatted(hubBasePath, slug);
    }
}
