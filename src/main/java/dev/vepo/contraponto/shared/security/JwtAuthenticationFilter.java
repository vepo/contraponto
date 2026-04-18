package dev.vepo.contraponto.shared.security;

import java.security.Principal;
import java.util.Set;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;

@Provider
@Secured
@Priority(Priorities.AUTHENTICATION)
public class JwtAuthenticationFilter implements ContainerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JWTParser jwtParser;

    @Inject
    public JwtAuthenticationFilter(JWTParser jwtParser) {
        this.jwtParser = jwtParser;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            // Don't abort for public endpoints - let them pass without authentication
            // Only abort if this is a protected endpoint
            if (isProtectedEndpoint(requestContext)) {
                logger.warn("Missing or invalid Authorization header for protected endpoint: {}",
                            requestContext.getUriInfo().getPath());
                abortWithUnauthorized(requestContext, "Missing or invalid authorization header");
            }
            return;
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();

        try {
            JsonWebToken validatedToken = jwtParser.parse(token);

            if (validatedToken.getExpirationTime() < System.currentTimeMillis() / 1000) {
                logger.warn("Expired token for subject: {}", validatedToken.getSubject());
                abortWithUnauthorized(requestContext, "Token has expired");
                return;
            }

            SecurityContext originalContext = requestContext.getSecurityContext();
            SecurityContext newContext = createSecurityContext(validatedToken, originalContext.isSecure());
            requestContext.setSecurityContext(newContext);

            // Also set the JWT token in the request context for CDI injection
            requestContext.setProperty("jwt", validatedToken);

            logger.debug("Successfully authenticated user: {}", validatedToken.getName());
        } catch (ParseException e) {
            logger.error("Failed to parse JWT token: {}", e.getMessage());
            if (isProtectedEndpoint(requestContext)) {
                abortWithUnauthorized(requestContext, "Invalid token");
            }
        } catch (Exception e) {
            logger.error("Unexpected error during token validation: {}", e.getMessage(), e);
            if (isProtectedEndpoint(requestContext)) {
                abortWithUnauthorized(requestContext, "Authentication failed");
            }
        }
    }

    private boolean isProtectedEndpoint(ContainerRequestContext requestContext) {
        // Check if the endpoint is annotated with @Secured
        // This is a simplified check - you might want to implement a more robust method
        String path = requestContext.getUriInfo().getPath();
        return !path.startsWith("/api/auth/") &&
                !path.startsWith("/style/") &&
                !path.startsWith("/js/") &&
                !path.equals("/") &&
                !path.startsWith("/post/");
    }

    private void abortWithUnauthorized(ContainerRequestContext requestContext, String message) {
        requestContext.abortWith(
                                 Response.status(Response.Status.UNAUTHORIZED)
                                         // .entity(new AuthEndpoint.ErrorResponse(message))
                                         .build());
    }

    private SecurityContext createSecurityContext(JsonWebToken token, boolean isSecure) {
        return new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return () -> token.getSubject();
            }

            @Override
            public boolean isUserInRole(String role) {
                Set<String> groups = token.getGroups();
                return groups != null && groups.contains(role);
            }

            @Override
            public boolean isSecure() {
                return isSecure;
            }

            @Override
            public String getAuthenticationScheme() {
                return "Bearer";
            }
        };
    }
}