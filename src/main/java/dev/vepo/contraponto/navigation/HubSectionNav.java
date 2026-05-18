package dev.vepo.contraponto.navigation;

public record HubSectionNav(String slug, String label) {

    public String sectionPath(NavigationHub hub) {
        return hub.path() + "/" + slug;
    }
}
