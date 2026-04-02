package dev.vepo.contraponto.shared.security;

import java.security.Principal;
import java.util.Set;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.auth.AuthEndpoint;
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
@Secured // This filter will run for all endpoints annotated with @Secured
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
        // Get the Authorization header
        String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);

        // Check if the Authorization header is present
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            logger.warn("Missing or invalid Authorization header for: {}", requestContext.getUriInfo().getPath());
            abortWithUnauthorized(requestContext, "Missing or invalid authorization header");
            return;
        }

        // Extract the token
        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();

        try {
            // Validate and parse the token
            JsonWebToken validatedToken = jwtParser.parse(token);

            // Check if token is expired
            if (validatedToken.getExpirationTime() < System.currentTimeMillis()) {
                logger.warn("Expired token for subject: {}", validatedToken.getSubject());
                abortWithUnauthorized(requestContext, "Token has expired");
                return;
            }

            // Create a new SecurityContext with the validated token
            SecurityContext originalContext = requestContext.getSecurityContext();
            SecurityContext newContext = createSecurityContext(validatedToken, originalContext.isSecure());
            requestContext.setSecurityContext(newContext);

            logger.debug("Successfully authenticated user: {}", validatedToken.getName());
        } catch (ParseException e) {
            logger.error("Failed to parse JWT token: {}", e.getMessage());
            abortWithUnauthorized(requestContext, "Invalid token");
        } catch (Exception e) {
            logger.error("Unexpected error during token validation: {}", e.getMessage(), e);
            abortWithUnauthorized(requestContext, "Authentication failed");
        }
    }

    private void abortWithUnauthorized(ContainerRequestContext requestContext, String message) {
        requestContext.abortWith(
                                 Response.status(Response.Status.UNAUTHORIZED)
                                         .entity(new AuthEndpoint.ErrorResponse(message))
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
                // Check if the token has the required role
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