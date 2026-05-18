package dev.vepo.contraponto.shared.infra;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import dev.vepo.contraponto.user.Role;
import dev.vepo.contraponto.user.User;

public class LoggedUser {

    private final User user;
    private final String sessionId;

    public LoggedUser() {
        this.user = null;
        this.sessionId = null;
    }

    public LoggedUser(User user, String sessionId) {
        this.user = user;
        this.sessionId = sessionId;
    }

    public String getAvatarUrl() {
        return AvatarUrls.avatarUrl(this);
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

    public long getId() {
        return Optional.ofNullable(user).map(User::getId).orElse(-1L);
    }

    public String getInitials() {
        return DisplayNameInitials.from(getName());
    }

    public String getName() {
        return Optional.ofNullable(user).map(User::getName).orElse("");
    }

    public Set<Role> getRoles() {
        return Optional.ofNullable(user).map(User::getRoles).orElse(Set.of());
    }

    public String getSessionId() {
        return Optional.ofNullable(sessionId).orElse("");
    }

    public User getUser() {
        return this.user;
    }

    public String getUsername() {
        return Optional.ofNullable(user).map(User::getUsername).orElse("");
    }

    public boolean hasRole(Role role) {
        return isAuthenticated() && user.hasRole(role);
    }

    public boolean isAuthenticated() {
        return Objects.nonNull(user);
    }

    public boolean isEditor() {
        return hasRole(Role.EDITOR) || hasRole(Role.ADMIN);
    }

    public boolean isUserAdministrator() {
        return hasRole(Role.USER_ADMINISTRATOR) || hasRole(Role.ADMIN);
    }

    @Override
    public String toString() {
        return "Image[user=%s, sessionId=%s]".formatted(user, sessionId);
    }
}
