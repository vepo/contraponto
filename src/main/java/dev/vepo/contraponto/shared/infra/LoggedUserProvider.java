package dev.vepo.contraponto.shared.infra;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.components.forms.LoginEndpoint;
import dev.vepo.contraponto.shared.security.SessionStore;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.user.UserRepository;
import io.vertx.core.http.HttpServerRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;

@ApplicationScoped
public class LoggedUserProvider {
    private static final Logger logger = LoggerFactory.getLogger(LoggedUserProvider.class);

    private final HttpServerRequest request;
    private final SessionStore sessionStore;
    private final UserRepository userRepository;

    @Inject
    public LoggedUserProvider(@Context HttpServerRequest request,
                              SessionStore sessionStore,
                              UserRepository userRepository) {
        this.request = request;
        this.sessionStore = sessionStore;
        this.userRepository = userRepository;
    }

    public Optional<LoggedUser> find(String sessionId) {
        return sessionStore.findUserId(sessionId)
                           .flatMap(userRepository::findByIdForSession)
                           .flatMap(user -> {
                               if (!user.isActive()) {
                                   sessionStore.remove(sessionId);
                                   return Optional.empty();
                               }
                               return Optional.of(new LoggedUser(user, sessionId));
                           });
    }

    public void invalidateAllSessionsForUser(long userId) {
        sessionStore.removeAllForUser(userId);
    }

    @Produces
    @RequestScoped
    public LoggedUser loadLoggedUser() {
        var cookie = request.getCookie(LoginEndpoint.SESSION_COOKIE_NAME);
        if (cookie == null) {
            return new LoggedUser();
        }
        var sessionId = cookie.getValue();
        return find(sessionId).map(user -> {
            logger.info("Logged cookie={} user={}", sessionId, user);
            return user;
        }).orElseGet(LoggedUser::new);
    }

    public LoggedUser login(User user) {
        var sessionId = UUID.randomUUID().toString();
        sessionStore.put(sessionId, user.getId());
        return new LoggedUser(user, sessionId);
    }

    public void logout(LoggedUser user) {
        sessionStore.remove(user.getSessionId());
    }

    public void update(String sessionId, User user) {
        sessionStore.put(sessionId, user.getId());
    }
}
