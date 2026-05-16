package dev.vepo.contraponto.user;

import java.util.Optional;
import java.util.regex.Pattern;

import dev.vepo.contraponto.custompage.CustomPagePaths;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UsernameValidator {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9_-]{2,19}$");

    public Optional<String> validate(String username) {
        if (username == null || username.isBlank()) {
            return Optional.of("Username is required.");
        }

        var normalized = username.trim();
        if (normalized.length() < 3 || normalized.length() > 20) {
            return Optional.of("Username must be between 3 and 20 characters.");
        }

        if (!USERNAME_PATTERN.matcher(normalized).matches()) {
            return Optional.of("Username must start with a letter or number and contain only letters, numbers, hyphens and underscores.");
        }

        if (CustomPagePaths.isReservedSegment(normalized.toLowerCase())) {
            return Optional.of("This username is reserved and cannot be used.");
        }

        return Optional.empty();
    }
}
