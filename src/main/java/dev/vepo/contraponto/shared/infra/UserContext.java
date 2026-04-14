package dev.vepo.contraponto.shared.infra;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.user.UserRepository;
import io.quarkus.qute.TemplateData;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;

@ApplicationScoped
public class UserContext {
    private static final Logger logger = LoggerFactory.getLogger(UserContext.class);

    @Inject
    JsonWebToken jwt;

    @Inject
    UserRepository userRepository;

    @Context
    SecurityContext securityContext;

    @Context
    UriInfo uriInfo;

    public UserInfo getCurrentUser() {
        // Try to get user from JsonWebToken first
        if (jwt != null && jwt.getSubject() != null && !jwt.getSubject().isEmpty()) {
            try {
                Long userId = Long.parseLong(jwt.getSubject());
                User user = userRepository.findById(userId);
                if (user != null && user.isActive()) {
                    logger.debug("User found via JWT: {}", user.getEmail());
                    return new UserInfo(user);
                }
            } catch (NumberFormatException e) {
                logger.warn("Invalid user ID in JWT subject: {}", jwt.getSubject());
            }
        }

        // Try to get user from SecurityContext as fallback
        if (securityContext != null && securityContext.getUserPrincipal() != null) {
            String principalName = securityContext.getUserPrincipal().getName();
            try {
                Long userId = Long.parseLong(principalName);
                User user = userRepository.findById(userId);
                if (user != null && user.isActive()) {
                    logger.debug("User found via SecurityContext: {}", user.getEmail());
                    return new UserInfo(user);
                }
            } catch (NumberFormatException e) {
                logger.warn("Invalid user ID in SecurityContext principal: {}", principalName);
            }
        }

        // For API endpoints that might have the token in the Authorization header
        // but not yet processed by the JWT filter
        String authHeader = getAuthorizationHeader();
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // This case should be handled by the JWT filter
            // Log for debugging
            logger.debug("Authorization header present but no JWT context available");
        }

        logger.debug("No authenticated user found for request: {}",
                     uriInfo != null ? uriInfo.getPath() : "unknown");
        return null;
    }

    private String getAuthorizationHeader() {
        // This is a workaround - in a real scenario, you'd need to access the request
        // headers
        // Since we can't inject HttpHeaders directly here due to scope issues,
        // we rely on the JWT filter to set up the SecurityContext
        return null;
    }

    @TemplateData
    public static class UserInfo {
        private final Long id;
        private final String name;
        private final String email;
        private final boolean authenticated;

        public UserInfo(User user) {
            this.id = user.getId();
            this.name = user.getName();
            this.email = user.getEmail();
            this.authenticated = true;
        }

        public UserInfo() {
            this.id = null;
            this.name = null;
            this.email = null;
            this.authenticated = false;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }

        public boolean isAuthenticated() {
            return authenticated;
        }

        public String getFirstName() {
            return name != null ? name.split(" ")[0] : "";
        }

        public String getInitials() {
            if (name == null || name.isEmpty())
                return "";
            String[] parts = name.trim().split("\\s+");
            if (parts.length == 1) {
                return parts[0].substring(0, 1).toUpperCase();
            }
            return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
        }

        public String getAvatarUrl() {
            // Generate avatar URL using UI Avatars service
            return "https://ui-avatars.com/api/?name=%s&background=1a8917&color=fff&bold=true&length=2".formatted(URLEncoder.encode(name,
                                                                                                                                    StandardCharsets.UTF_8));
        }

        @Override
        public String toString() {
            return "UserInfo[id=%d, name=%s, email=%s, authenticated=%b]".formatted(id, name, email, authenticated);
        }
    }
}