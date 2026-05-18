package dev.vepo.contraponto.git;

public record GitSyncRunResult(GitSyncOutcome outcome,
                               GitErrorKind gitErrorKind,
                               boolean repositoryReadable,
                               boolean dataLoadable,
                               String commitAfter,
                               String conventionSnapshot,
                               String settingsSnapshot,
                               String summaryMessage,
                               String errorDetail) {}
