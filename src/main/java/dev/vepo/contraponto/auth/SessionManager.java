package dev.vepo.contraponto.auth;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import dev.vepo.contraponto.user.User;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SessionManager {

    private final Map<String, String> sessionData;
    private final Map<String, User> loggedUsers;

    public SessionManager() {
        this.loggedUsers = Collections.synchronizedMap(new HashMap<>());
        this.sessionData = Collections.synchronizedMap(new HashMap<>());
    }

    public String login(String token, User user) {
        var sessionId = UUID.randomUUID().toString();
        this.loggedUsers.put(token, user);
        this.sessionData.put(sessionId, token);
        return sessionId;
    }

}
