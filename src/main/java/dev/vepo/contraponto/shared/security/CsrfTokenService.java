package dev.vepo.contraponto.shared.security;

import java.util.Map;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Cookie;

/**
 * Double-submit CSRF token exposed to templates and validated on mutating
 * requests.
 */
@ApplicationScoped
public class CsrfTokenService {

    public static final String COOKIE_NAME = "__csrf";
    public static final String HEADER_NAME = "X-CSRF-Token";

    public String newToken() {
        return UUID.randomUUID().toString();
    }

    public String readToken(Map<String, Cookie> cookies) {
        if (cookies == null) {
            return "";
        }
        Cookie cookie = cookies.get(COOKIE_NAME);
        if (cookie != null && cookie.getValue() != null && !cookie.getValue().isBlank()) {
            return cookie.getValue();
        }
        return "";
    }
}
