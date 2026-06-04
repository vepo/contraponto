package dev.vepo.contraponto.shared.infra;

import java.util.Locale;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * White-label platform name for UI, SEO, RSS, and transactional copy. Override
 * at deploy time via {@code APP_SITE_NAME} (maps to {@code app.site.name}).
 */
@ApplicationScoped
public class SiteBranding {

    static String seoNameFrom(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Contraponto";
        }
        if ("contraponto".equalsIgnoreCase(raw.trim())) {
            return "Contraponto";
        }
        String[] parts = raw.trim().split("[-_]+");
        var sb = new StringBuilder();
        for (var part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                sb.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return sb.isEmpty() ? raw.trim() : sb.toString();
    }

    private final String displayName;

    @Inject
    public SiteBranding(@ConfigProperty(name = "app.site.name", defaultValue = "contraponto") String displayName) {
        this.displayName = displayName == null || displayName.isBlank() ? "contraponto" : displayName.trim();
    }

    /** Logo, footer, and page title suffix (as configured). */
    public String displayName() {
        return displayName;
    }

    /** Title case for SEO, Open Graph, and email subjects. */
    public String seoName() {
        return seoNameFrom(displayName);
    }
}
