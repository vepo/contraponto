package dev.vepo.contraponto.shared.infra;

import java.time.LocalDateTime;

import dev.vepo.contraponto.shared.htmx.HtmxTriggers;
import io.quarkus.qute.TemplateGlobal;

@TemplateGlobal
public class Globals {

    @TemplateGlobal(name = "authRefreshTrigger")
    public static String authRefreshTrigger() {
        return HtmxTriggers.AUTH_REFRESH_TRIGGER;
    }

    @TemplateGlobal(name = "currentYear")
    public static int currentYear() {
        return LocalDateTime.now().getYear();
    }

    private Globals() {
        /* This utility class should not be instantiated */
    }
}
