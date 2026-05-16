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
}
