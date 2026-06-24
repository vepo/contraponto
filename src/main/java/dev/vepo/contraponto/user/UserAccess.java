package dev.vepo.contraponto.user;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import dev.vepo.contraponto.user.LoggedUser;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserAccess {

    public List<Role> assignableRoles(LoggedUser actor) {
        return Arrays.stream(Role.values())
                     .filter(role -> canAssignRole(actor, role))
                     .toList();
    }

    public boolean canAssignRole(LoggedUser actor, Role role) {
        if (!canManageUsers(actor)) {
            return false;
        }
        if (actor.hasRole(Role.ADMIN)) {
            return true;
        }
        return role != Role.ADMIN;
    }

    public boolean canManageUsers(LoggedUser user) {
        return user.isAuthenticated() && (user.hasRole(Role.USER_ADMINISTRATOR) || user.hasRole(Role.ADMIN));
    }

    public Set<Role> parseRoles(LoggedUser actor, List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return Set.of(Role.USER);
        }

        return roleNames.stream()
                        .map(String::trim)
                        .filter(name -> !name.isBlank())
                        .map(Role::valueOf)
                        .filter(role -> canAssignRole(actor, role))
                        .collect(Collectors.toSet());
    }
}
