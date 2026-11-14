package dev.vepo.contraponto.shared.security;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.ext.RuntimeDelegate;
import jakarta.ws.rs.ext.RuntimeDelegate.HeaderDelegate;

import java.util.Optional;

@ApplicationScoped
public class SessionCookieSupport {

    private final boolean secureCookies;
    private final long ttlSeconds;
    private final String cookieDomain;
    private final HeaderDelegate<NewCookie> cookieSerializer;

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
        this.cookieSerializer = RuntimeDelegate.getInstance().createHeaderDelegate(NewCookie.class);
    }

    public String buildClearSessionCookie() {
        return  cookieSerializer.toString(buildClearSessionNewCookie());
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

    public NewCookie buildCsrfNewCookie(String token) {
        var builder = new NewCookie.Builder(CsrfTokenService.COOKIE_NAME).value(token)
                                                                         .path("/")
                                                                         .httpOnly(false)
                                                                         .secure(secureCookies)
                                                                         .sameSite(NewCookie.SameSite.LAX);
        if (cookieDomain != null) {
            builder.domain(cookieDomain);
        }
        return builder.build();
    }

    public String buildSessionCookie(String sessionId) {
        return cookieSerializer.toString(buildSessionNewCookie(sessionId));
    }

    public NewCookie buildSessionNewCookie(String sessionId) {
        var builder = new NewCookie.Builder(SessionConstants.SESSION_COOKIE_NAME)
                                   .value(sessionId)
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
