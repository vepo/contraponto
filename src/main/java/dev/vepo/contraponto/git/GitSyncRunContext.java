package dev.vepo.contraponto.git;

/**
 * Holds the active {@link GitSyncRun} id for the current async Git integration
 * thread.
 */
public final class GitSyncRunContext {

    private static final ThreadLocal<Long> ACTIVE_RUN = new ThreadLocal<>();

    public static void clear() {
        ACTIVE_RUN.remove();
    }

    public static Long currentRunId() {
        return ACTIVE_RUN.get();
    }

    public static void setRunId(long runId) {
        ACTIVE_RUN.set(runId);
    }

    private GitSyncRunContext() {}
}
