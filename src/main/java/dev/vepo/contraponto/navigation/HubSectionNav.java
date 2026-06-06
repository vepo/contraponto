package dev.vepo.contraponto.navigation;

public record HubSectionNav(String slug, String label, String i18nKey) {

    public HubSectionNav(String slug, String label) {
        this(slug, label, null);
    }

    public String sectionPath(NavigationHub hub) {
        return "%s/%s".formatted(hub.path(), slug);
    }
}
