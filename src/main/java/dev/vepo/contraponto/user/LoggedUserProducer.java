package dev.vepo.contraponto.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.shared.security.SessionConstants;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;

@RequestScoped
public class LoggedUserProducer {

    private static final Logger logger = LoggerFactory.getLogger(LoggedUserProducer.class);

    private final ContainerRequestContext requestContext;

    private final LoggedUserProvider loggedUserProvider;

    @Inject
    public LoggedUserProducer(ContainerRequestContext requestContext, LoggedUserProvider loggedUserProvider) {
        this.requestContext = requestContext;
        this.loggedUserProvider = loggedUserProvider;
    }

    @Produces
    @RequestScoped
    public LoggedUser loggedUser() {
        var sessionId = sessionIdFromRequest();
        if (sessionId == null) {
            return new LoggedUser();
        }
        return loggedUserProvider.find(sessionId)
                                 .map(user -> {
                                     logger.info("Logged cookie={} user={}", sessionId, user);
                                     return user;
                                 })
                                 .orElseGet(LoggedUser::new);
    }

    private String sessionIdFromRequest() {
        var cookie = requestContext.getCookies().get(SessionConstants.SESSION_COOKIE_NAME);
        if (cookie == null || cookie.getValue() == null || cookie.getValue().isBlank()) {
            return null;
        }
        return cookie.getValue();
    }
}
