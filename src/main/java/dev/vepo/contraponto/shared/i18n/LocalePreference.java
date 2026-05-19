package dev.vepo.contraponto.shared.i18n;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LocalePreference {

    public static final String COOKIE_NAME = "contraponto_locale";
    public static final String DEFAULT_LOCALE = "pt-BR";
    private static final Set<String> SUPPORTED = Set.of("pt-BR", "en", "es");

    private static final Map<String, String> BCP47 = Map.of(
                                                            "pt-BR", "pt-BR",
                                                            "en", "en",
                                                            "es", "es");

    public boolean isSupported(String locale) {
        return SUPPORTED.contains(normalize(locale));
    }

    public String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT_LOCALE;
        }
        var trimmed = raw.trim();
        if ("pt".equalsIgnoreCase(trimmed) || "pt-br".equalsIgnoreCase(trimmed)) {
            return DEFAULT_LOCALE;
        }
        for (var locale : SUPPORTED) {
            if (locale.equalsIgnoreCase(trimmed)) {
                return locale;
            }
        }
        return DEFAULT_LOCALE;
    }

    public Set<String> supportedLocales() {
        return SUPPORTED;
    }

    public String toBcp47(String locale) {
        return BCP47.getOrDefault(normalize(locale), "pt-BR");
    }
}
