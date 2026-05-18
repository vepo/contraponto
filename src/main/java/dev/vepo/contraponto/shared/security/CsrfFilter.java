package dev.vepo.contraponto.shared.security;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
public class CsrfFilter implements ContainerRequestFilter {

    private static final Set<String> MUTATING = Set.of("POST", "PUT", "DELETE", "PATCH");

    private static String readCookie(Map<String, Cookie> cookies) {
        if (cookies == null) {
            return "";
        }
        Cookie cookie = cookies.get(CsrfTokenService.COOKIE_NAME);
        return cookie != null && cookie.getValue() != null ? cookie.getValue() : "";
    }

    private static boolean requiresCsrf(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        if (normalized.equals("forms/auth/login") || normalized.equals("forms/auth/signup")) {
            return false;
        }
        return normalized.startsWith("forms/") || normalized.startsWith("api/images");
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (!MUTATING.contains(requestContext.getMethod())) {
            return;
        }
        String path = requestContext.getUriInfo().getPath();
        if (!requiresCsrf(path)) {
            return;
        }
        if (isValid(requestContext)) {
            return;
        }
        requestContext.abortWith(Response.status(Response.Status.FORBIDDEN)
                                         .entity("Invalid or missing CSRF token.")
                                         .build());
    }

    private boolean isValid(ContainerRequestContext requestContext) {
        String cookieToken = readCookie(requestContext.getCookies());
        if (cookieToken.isBlank()) {
            Object requestToken = requestContext.getProperty(CsrfRequestSetupFilter.TOKEN_PROPERTY);
            cookieToken = requestToken != null ? requestToken.toString() : "";
        }
        String headerToken = requestContext.getHeaderString(CsrfTokenService.HEADER_NAME);
        return !cookieToken.isBlank() && Objects.equals(cookieToken, headerToken);
    }
}
