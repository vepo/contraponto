package dev.vepo.contraponto.user;

public enum Role {
    USER,
    EDITOR,
    USER_ADMINISTRATOR,
    ADMIN;

    public String label() {
        return switch (this) {
            case USER -> "User";
            case EDITOR -> "Editor";
            case USER_ADMINISTRATOR -> "User administrator";
            case ADMIN -> "Administrator";
        };
    }

    public String description() {
        return switch (this) {
            case USER -> "Write and publish on their own blogs.";
            case EDITOR -> "Feature posts on the home page and manage the review queue and tags.";
            case USER_ADMINISTRATOR -> "Create and manage user accounts and assign roles (except Administrator).";
            case ADMIN -> "Full platform access, including assigning the Administrator role.";
        };
    }
}
