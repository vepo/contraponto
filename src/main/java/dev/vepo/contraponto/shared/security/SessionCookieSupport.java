package dev.vepo.contraponto.shared.security;

import org.eclipse.microprofile.config.inject.ConfigProperty;

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
        var cookie = "%s=%s; Path=/; Max-Age=%d; HttpOnly; SameSite=Lax".formatted(SessionConstants.SESSION_COOKIE_NAME,
                                                                                   sessionId,
                                                                                   ttlSeconds);
        if (secureCookies) {
            return "%s; Secure".formatted(cookie);
        }
        return cookie;
    }
}
