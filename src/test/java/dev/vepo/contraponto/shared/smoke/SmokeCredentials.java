package dev.vepo.contraponto.shared.smoke;

/** Fixed credentials for docker-smoke stack (not production secrets). */
public final class SmokeCredentials {

    public static final String POSTGRES_PASSWORD = "SmokeTestDbSecret1";

    public static final String PASSWORD_SALT = "SmokeTestSalt1";

    public static final String ADMIN_USERNAME = "admin";

    public static final String ADMIN_PASSWORD = "SmokeAdminPass1";

    private SmokeCredentials() {}
}
