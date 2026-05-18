package dev.vepo.contraponto.navigation;

import java.util.List;

public record BreadcrumbTrail(List<BreadcrumbItem> items) {

    public static final BreadcrumbTrail EMPTY = new BreadcrumbTrail(List.of());

    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }
}
