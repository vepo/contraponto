package dev.vepo.contraponto.shared.infra;

import java.time.LocalDateTime;

import dev.vepo.contraponto.components.forms.LoginEndpoint;
import io.quarkus.qute.TemplateGlobal;

@TemplateGlobal
public class Globals {

    @TemplateGlobal(name = "currentYear")
    public static int currentYear() {
        return LocalDateTime.now().getYear();
    }

    @TemplateGlobal(name = "sessionCookieKey")
    public static String sessionCookieKey() {
        return LoginEndpoint.SESSION_COOKIE_NAME;
    }

    private Globals() {
        /* This utility class should not be instantiated */
    }
}
