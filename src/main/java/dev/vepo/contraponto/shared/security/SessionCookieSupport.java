package dev.vepo.contraponto.shared.security;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Optional;

@ApplicationScoped
public class SessionCookieSupport {

    private final boolean secureCookies;
    private final long ttlSeconds;
    private final String cookieDomain;

    @Inject
    public SessionCookieSupport(@ConfigProperty(name = "app.secure-cookies", defaultValue = "false") boolean secureCookies,
                                @ConfigProperty(name = "app.session.ttl-seconds", defaultValue = "2592000") long ttlSeconds,
                                @ConfigProperty(name = "app.session.cookie-domain") Optional<String> cookieDomain) {
        this(secureCookies, ttlSeconds, cookieDomain.filter(domain -> !domain.isBlank()).orElse(null));
    }

    SessionCookieSupport(boolean secureCookies, long ttlSeconds, String cookieDomain) {
        this.secureCookies = secureCookies;
        this.ttlSeconds = ttlSeconds;
        this.cookieDomain = cookieDomain;
    }

    public String buildSessionCookie(String sessionId) {
        var cookie = "%s=%s; Path=/; Max-Age=%d; HttpOnly; SameSite=Lax".formatted(SessionConstants.SESSION_COOKIE_NAME,
                                                                                   sessionId,
                                                                                   ttlSeconds);
        if (cookieDomain != null) {
            cookie = "%s; Domain=%s".formatted(cookie, cookieDomain);
        }
        if (secureCookies) {
            return "%s; Secure".formatted(cookie);
        }
        return cookie;
    }
}
