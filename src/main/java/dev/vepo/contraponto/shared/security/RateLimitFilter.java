package dev.vepo.contraponto.shared.security;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * Simple in-memory rate limiter for sensitive auth and comment endpoints.
 */
@Provider
public class RateLimitFilter implements ContainerRequestFilter {

    private static final record Window(Instant resetAt, AtomicInteger count) {}

    private static String clientKey(ContainerRequestContext context) {
        String forwarded = context.getHeaderString("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return context.getHeaderString("X-Real-IP") != null ? context.getHeaderString("X-Real-IP") : "unknown";
    }

    private static String normalize(String path) {
        return path.startsWith("/") ? path.substring(1) : path;
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    private final int maxRequests;

    private final int windowSeconds;

    @Inject
    public RateLimitFilter(@ConfigProperty(name = "app.rate-limit.max-requests", defaultValue = "30") int maxRequests,
                           @ConfigProperty(name = "app.rate-limit.window-seconds", defaultValue = "60") int windowSeconds) {
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (!"POST".equalsIgnoreCase(requestContext.getMethod())) {
            return;
        }
        String path = requestContext.getUriInfo().getPath();
        if (!isLimited(path)) {
            return;
        }
        String key = clientKey(requestContext) + ":" + normalize(path);
        if (isOverLimit(key)) {
            requestContext.abortWith(Response.status(Response.Status.TOO_MANY_REQUESTS)
                                             .entity("Too many requests. Please try again later.")
                                             .build());
        }
    }

    private boolean isLimited(String path) {
        if (path == null) {
            return false;
        }
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        return normalized.equals("forms/auth/login")
                || normalized.equals("forms/auth/signup")
                || normalized.startsWith("forms/posts/") && normalized.endsWith("/comments");
    }

    private boolean isOverLimit(String key) {
        Instant now = Instant.now();
        Window window = windows.compute(key, (k, existing) -> {
            if (existing == null || now.isAfter(existing.resetAt())) {
                return new Window(now.plusSeconds(windowSeconds), new AtomicInteger(0));
            }
            return existing;
        });
        return window.count.incrementAndGet() > maxRequests;
    }
}
