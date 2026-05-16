package dev.vepo.contraponto.user;

import java.util.stream.Collectors;

public record UserRow(Long id,
                      String username,
                      String name,
                      String email,
                      boolean active,
                      String rolesLabel) {

    public static UserRow from(User user) {
        var rolesLabel = user.getRoles()
                             .stream()
                             .map(Role::label)
                             .sorted()
                             .collect(Collectors.joining(", "));
        return new UserRow(user.getId(),
                           user.getUsername(),
                           user.getName(),
                           user.getEmail(),
                           user.isActive(),
                           rolesLabel);
    }
}
