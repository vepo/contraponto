package dev.vepo.contraponto.shared.infra;

import java.time.LocalDateTime;

import dev.vepo.contraponto.shared.htmx.HtmxTriggers;
import dev.vepo.contraponto.shared.security.CurrentCsrfToken;
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
        var current = CDI.current().select(CurrentCsrfToken.class);
        if (!current.isResolvable()) {
            return "";
        }
        return current.get().get();
    }

    @TemplateGlobal(name = "currentYear")
    public static int currentYear() {
        return LocalDateTime.now().getYear();
    }

    @TemplateGlobal(name = "session")
    public static LoggedUser session() {
        var loggedUser = CDI.current().select(LoggedUser.class);
        if (!loggedUser.isResolvable()) {
            return new LoggedUser();
        }
        return loggedUser.get();
    }

    private Globals() {
        /* This utility class should not be instantiated */
    }
}
