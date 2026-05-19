package dev.vepo.contraponto.view;

import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.NewCookie;

@ApplicationScoped
public class SessionIdProvider {

    public static final String VIEW_SESSION_COOKIE = "__view_session";

    private final boolean secureCookies;

    public SessionIdProvider(@ConfigProperty(name = "app.secure-cookies", defaultValue = "false") boolean secureCookies) {
        this.secureCookies = secureCookies;
    }

    public NewCookie createSessionCookie(String sessionId) {
        return new NewCookie.Builder(VIEW_SESSION_COOKIE)
                                                         .value(sessionId)
                                                         .path("/")
                                                         .maxAge(365 * 24 * 3600) // 1 year
                                                         .httpOnly(false) // accessible by JS (optional)
                                                         .secure(secureCookies)
                                                         .sameSite(NewCookie.SameSite.LAX)
                                                         .build();
    }

    public String getOrCreateSessionId(Cookie cookie) {
        if (cookie != null && cookie.getValue() != null && !cookie.getValue().isBlank()) {
            return cookie.getValue();
        }
        return UUID.randomUUID().toString();
    }
}
