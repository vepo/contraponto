package dev.vepo.contraponto.shared.security;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import dev.vepo.contraponto.components.forms.LoginEndpoint;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SessionCookieSupport {

    private final boolean secureCookies;
    private final long ttlSeconds;

    public SessionCookieSupport(@ConfigProperty(name = "app.secure-cookies", defaultValue = "false") boolean secureCookies,
                                @ConfigProperty(name = "app.session.ttl-seconds", defaultValue = "2592000") long ttlSeconds) {
        this.secureCookies = secureCookies;
        this.ttlSeconds = ttlSeconds;
    }

    public String buildSessionCookie(String sessionId) {
        var builder = new StringBuilder();
        builder.append("%s=%s; Path=/; Max-Age=%d; HttpOnly; SameSite=Lax".formatted(LoginEndpoint.SESSION_COOKIE_NAME,
                                                                                     sessionId,
                                                                                     ttlSeconds));
        if (secureCookies) {
            builder.append("; Secure");
        }
        return builder.toString();
    }
}
