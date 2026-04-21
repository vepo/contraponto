package dev.vepo.contraponto.shared.infra;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.user.User;
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
    private final Map<String, User> sessions;

    @Inject
    public LoggedUserProvider(@Context HttpServerRequest request) {
        this.request = request;
        this.sessions = Collections.synchronizedMap(new HashMap<>());
    }

    @Produces
    @RequestScoped
    public LoggedUser loadLoggedUser() {
        var cookie = request.getCookie("__session");
        logger.info("New logged user! cookie={}", cookie);
        if (Objects.nonNull(cookie)) {
            var sessionId = cookie.getValue();
            var user = sessions.get(sessionId);
            if (Objects.nonNull(user)) {
                logger.info("Logged cookie={} user={}", sessionId, user);
                return new LoggedUser(user, sessionId);
            }
        }

        return new LoggedUser();
    }

    public LoggedUser login(User user) {
        var sessionId = UUID.randomUUID().toString();
        this.sessions.put(sessionId, user);
        return new LoggedUser(user, sessionId);
    }

    public void logout(LoggedUser user) {
        this.sessions.remove(user.getSessionId());
    }

    public Optional<LoggedUser> find(String sessionId) {
        return Optional.ofNullable(this.sessions.get(sessionId))
                       .map(user -> new LoggedUser(user, sessionId));
    }

    public void update(String sessionId, User user) {
        this.sessions.put(sessionId, user);
    }
}
