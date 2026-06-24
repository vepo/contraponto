package dev.vepo.contraponto.shared.security;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.NewCookie;
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

    public String buildClearSessionCookie() {
        return buildClearSessionNewCookie().toString();
    }

    public NewCookie buildClearSessionNewCookie() {
        var builder = new NewCookie.Builder(SessionConstants.SESSION_COOKIE_NAME)
                                                                                 .value("")
                                                                                 .path("/")
                                                                                 .maxAge(0)
                                                                                 .httpOnly(true)
                                                                                 .secure(secureCookies)
                                                                                 .sameSite(NewCookie.SameSite.LAX);
        if (cookieDomain != null) {
            builder.domain(cookieDomain);
        }
        return builder.build();
    }

    public String buildSessionCookie(String sessionId) {
        return buildSessionNewCookie(sessionId).toString();
    }

    public NewCookie buildSessionNewCookie(String sessionId) {
        var builder = new NewCookie.Builder(SessionConstants.SESSION_COOKIE_NAME).value(sessionId)
                                                                                 .path("/")
                                                                                 .maxAge(Math.toIntExact(ttlSeconds))
                                                                                 .httpOnly(true)
                                                                                 .secure(secureCookies)
                                                                                 .sameSite(NewCookie.SameSite.LAX);
        if (cookieDomain != null) {
            builder.domain(cookieDomain);
        }
        return builder.build();
    }
}
