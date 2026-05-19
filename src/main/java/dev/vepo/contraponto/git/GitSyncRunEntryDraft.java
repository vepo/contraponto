package dev.vepo.contraponto.git;

public record GitSyncRunEntryDraft(GitSyncPhase phase,
                                   GitSyncEntryLevel level,
                                   Long postId,
                                   String markdownPath,
                                   GitSyncEntryOutcome outcome,
                                   String message,
                                   String remediation,
                                   String technicalDetail) {

    public static GitSyncRunEntryDraft info(GitSyncPhase phase, String message) {
        return new GitSyncRunEntryDraft(phase, GitSyncEntryLevel.INFO, null, null,
                                        GitSyncEntryOutcome.SUCCESS, message, null, null);
    }

    public static GitSyncRunEntryDraft warn(GitSyncPhase phase, String message, String remediation) {
        return new GitSyncRunEntryDraft(phase, GitSyncEntryLevel.WARN, null, null,
                                        GitSyncEntryOutcome.SKIPPED, message, remediation, null);
    }

    public static GitSyncRunEntryDraft warnPost(GitSyncPhase phase,
                                                Long postId,
                                                String markdownPath,
                                                String message,
                                                String remediation) {
        return new GitSyncRunEntryDraft(phase, GitSyncEntryLevel.WARN, postId, markdownPath,
                                        GitSyncEntryOutcome.SUCCESS, message, remediation, null);
    }

    public static GitSyncRunEntryDraft error(GitSyncPhase phase,
                                             String message,
                                             String remediation,
                                             String technicalDetail) {
        return new GitSyncRunEntryDraft(phase, GitSyncEntryLevel.ERROR, null, null,
                                        GitSyncEntryOutcome.FAILED, message, remediation, technicalDetail);
    }
}
