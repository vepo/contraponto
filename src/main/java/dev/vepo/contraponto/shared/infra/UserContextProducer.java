package dev.vepo.contraponto.shared.infra;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.shared.infra.UserContext.UserInfo;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.user.UserRepository;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;

@RequestScoped
public class UserContextProducer {
    private static final Logger logger = LoggerFactory.getLogger(UserContextProducer.class);

    @Inject
    JsonWebToken jwt;

    @Inject
    UserRepository userRepository;

    @Context
    SecurityContext securityContext;

    private User currentUser;
    private UserInfo currentUserInfo;

    @Inject
    SecurityIdentity identity;

    @Produces
    @RequestScoped
    public UserInfo produceUserInfo() {
        logger.info("Logged {}", identity.getPrincipal());
        if (currentUserInfo != null) {
            return currentUserInfo;
        }

        User user = getCurrentUser();
        if (user != null) {
            currentUserInfo = new UserInfo(user);
            return currentUserInfo;
        }

        return new UserInfo();
    }

    @Produces
    @RequestScoped
    public User produceUser() {
        if (currentUser != null) {
            return currentUser;
        }

        currentUser = getCurrentUser();
        return currentUser;
    }

    private User getCurrentUser() {
        // Try JWT first
        if (jwt != null && jwt.getSubject() != null && !jwt.getSubject().isEmpty()) {
            try {
                Long userId = Long.parseLong(jwt.getSubject());
                User user = userRepository.findById(userId);
                if (user != null && user.isActive()) {
                    logger.debug("User resolved via JWT: {}", user.getEmail());
                    return user;
                }
            } catch (NumberFormatException e) {
                logger.warn("Invalid user ID in JWT: {}", jwt.getSubject());
            }
        }

        // Try SecurityContext
        if (securityContext != null && securityContext.getUserPrincipal() != null) {
            String principalName = securityContext.getUserPrincipal().getName();
            try {
                Long userId = Long.parseLong(principalName);
                User user = userRepository.findById(userId);
                if (user != null && user.isActive()) {
                    logger.debug("User resolved via SecurityContext: {}", user.getEmail());
                    return user;
                }
            } catch (NumberFormatException e) {
                logger.warn("Invalid user ID in SecurityContext: {}", principalName);
            }
        }

        return null;
    }
}