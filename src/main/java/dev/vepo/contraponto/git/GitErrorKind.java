package dev.vepo.contraponto.git;

public enum GitErrorKind {
    NONE,
    AUTHENTICATION,
    NETWORK,
    REPOSITORY,
    WORKSPACE,
    CONVENTION,
    POST,
    UNKNOWN
}
