package dev.vepo.contraponto.navigation;

public record BreadcrumbItem(String label, String href) {

    public boolean isCurrent() {
        return href == null || href.isBlank();
    }
}
