package dev.vepo.contraponto.shared.infra;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

import dev.vepo.contraponto.user.User;

public class LoggedUser {

    private final User user;
    private final String sessionId;

    public LoggedUser(User user, String sessionId) {
        this.user = user;
        this.sessionId = sessionId;
    }

    public LoggedUser() {
        this.user = null;
        this.sessionId = null;
    }

    public boolean isAuthenticated() {
        return Objects.nonNull(user);
    }

    public User getUser() {
        return this.user;
    }

    public long getId() {
        return Optional.ofNullable(user).map(User::getId).orElse(-1l);
    }

    public String getUsername() {
        return Optional.ofNullable(user).map(User::getUsername).orElse("");
    }

    public String getName() {
        return Optional.ofNullable(user).map(User::getName).orElse("");
    }

    public String getEmail() {
        return Optional.ofNullable(user).map(User::getEmail).orElse("");
    }

    public String getFirstName() {
        return Optional.ofNullable(user)
                       .map(User::getName)
                       .map(name -> name.split(" ")[0])
                       .orElse("");
    }

    public String getInitials() {
        return Optional.ofNullable(user)
                       .map(User::getName)
                       .map(name -> {
                           var parts = name.trim().split("\\s+");
                           if (parts.length == 1) {
                               return parts[0].substring(0, 1).toUpperCase();
                           }
                           return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
                       })
                       .orElse("");
    }

    public String getAvatarUrl() {
        return Optional.ofNullable(user)
                       .map(User::getName)
                       // Generate avatar URL using UI Avatars service
                       .map(name -> "https://ui-avatars.com/api/?name=%s&background=1a8917&color=fff&bold=true&length=2".formatted(URLEncoder.encode(name,
                                                                                                                                                     StandardCharsets.UTF_8)))
                       .orElse("");

    }

    public String getSessionId() {
        return Optional.ofNullable(sessionId).orElse("");
    }

    @Override
    public String toString() {
        return "Image[user=%s, sessionId=%s]".formatted(user, sessionId);
    }
}
