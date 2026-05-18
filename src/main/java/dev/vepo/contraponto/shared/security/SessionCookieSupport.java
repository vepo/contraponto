package dev.vepo.contraponto.shared.security;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import dev.vepo.contraponto.components.forms.LoginEndpoint;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SessionCookieSupport {

    private final boolean secureCookies;

    public SessionCookieSupport(@ConfigProperty(name = "app.secure-cookies", defaultValue = "false") boolean secureCookies) {
        this.secureCookies = secureCookies;
    }

    public String buildSessionCookie(String sessionId) {
        var builder = new StringBuilder();
        builder.append("%s=%s; Path=/; HttpOnly; SameSite=Strict".formatted(LoginEndpoint.SESSION_COOKIE_NAME,
                                                                            sessionId));
        if (secureCookies) {
            builder.append("; Secure");
        }
        return builder.toString();
    }
}
