package dev.vepo.contraponto.shared.infra;

import java.time.LocalDateTime;

import dev.vepo.contraponto.shared.htmx.HtmxTriggers;
import dev.vepo.contraponto.shared.security.CsrfTokenResolver;
import io.quarkus.qute.TemplateGlobal;
import jakarta.enterprise.inject.spi.CDI;

@TemplateGlobal
public class Globals {

    @TemplateGlobal(name = "authRefreshTrigger")
    public static String authRefreshTrigger() {
        return HtmxTriggers.AUTH_REFRESH_TRIGGER;
    }

    @TemplateGlobal(name = "csrfToken")
    public static String csrfToken() {
        var resolver = CDI.current().select(CsrfTokenResolver.class);
        if (!resolver.isResolvable()) {
            return "";
        }
        return resolver.get().currentToken();
    }

    @TemplateGlobal(name = "currentYear")
    public static int currentYear() {
        return LocalDateTime.now().getYear();
    }

    private Globals() {
        /* This utility class should not be instantiated */
    }
}
