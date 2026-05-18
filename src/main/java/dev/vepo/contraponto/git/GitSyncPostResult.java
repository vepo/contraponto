package dev.vepo.contraponto.git;

public record GitSyncPostResult(GitSyncEntryOutcome outcome,
                                Long postId,
                                String markdownPath,
                                String message,
                                String remediation,
                                String technicalDetail) {

    public static GitSyncPostResult success(Long postId, String markdownPath, String message) {
        return new GitSyncPostResult(GitSyncEntryOutcome.SUCCESS, postId, markdownPath, message, null, null);
    }

    public static GitSyncPostResult skipped(String markdownPath, String message, String remediation) {
        return new GitSyncPostResult(GitSyncEntryOutcome.SKIPPED, null, markdownPath, message, remediation, null);
    }

    public static GitSyncPostResult failed(String markdownPath,
                                           String message,
                                           String remediation,
                                           String technicalDetail) {
        return new GitSyncPostResult(GitSyncEntryOutcome.FAILED, null, markdownPath, message, remediation, technicalDetail);
    }

    public boolean isFailed() {
        return outcome == GitSyncEntryOutcome.FAILED;
    }

    public boolean isSuccess() {
        return outcome == GitSyncEntryOutcome.SUCCESS;
    }
}
