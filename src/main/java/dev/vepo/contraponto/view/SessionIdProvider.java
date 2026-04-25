package dev.vepo.contraponto.view;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.NewCookie;

@ApplicationScoped
public class SessionIdProvider {

    public static final String VIEW_SESSION_COOKIE = "__view_session";

    public String getOrCreateSessionId(Cookie cookie) {
        if (cookie != null && cookie.getValue() != null && !cookie.getValue().isBlank()) {
            return cookie.getValue();
        }
        return UUID.randomUUID().toString();
    }

    public NewCookie createSessionCookie(String sessionId) {
        return new NewCookie.Builder(VIEW_SESSION_COOKIE)
                                                         .value(sessionId)
                                                         .path("/")
                                                         .maxAge(365 * 24 * 3600) // 1 year
                                                         .httpOnly(false) // accessible by JS (optional)
                                                         .secure(false) // set true in production if using HTTPS
                                                         .sameSite(NewCookie.SameSite.LAX)
                                                         .build();
    }
}