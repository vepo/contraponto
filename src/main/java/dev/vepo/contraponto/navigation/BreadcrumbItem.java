package dev.vepo.contraponto.navigation;

public record BreadcrumbItem(String label, String href, String i18nKey) {

    public BreadcrumbItem(String label, String href) {
        this(label, href, null);
    }

    public boolean isCurrent() {
        return href == null || href.isBlank();
    }
}
