package dev.vepo.contraponto.git;

public enum GitSyncPhase {
    WORKSPACE,
    FETCH,
    PULL,
    CONVENTION,
    ASSETS,
    POST_EXPORT,
    POST_IMPORT,
    COMMIT,
    PUSH
}
