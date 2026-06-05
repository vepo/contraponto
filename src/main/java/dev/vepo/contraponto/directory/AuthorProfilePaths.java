package dev.vepo.contraponto.directory;

import dev.vepo.contraponto.user.User;

public final class AuthorProfilePaths {

    public static String url(User author) {
        return "/authors/%s".formatted(author.getUsername());
    }

    private AuthorProfilePaths() {}
}
