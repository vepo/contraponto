package dev.vepo.contraponto.shared.infra;

/**
 * Supplies footer navigation links for error pages and other shared layouts
 * without depending on custom page persistence types in the shared kernel.
 */
public interface FooterLinksProvider {

    Object loadGlobalLinks();
}
