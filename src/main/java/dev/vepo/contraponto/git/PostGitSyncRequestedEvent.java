package dev.vepo.contraponto.git;

/**
 * Fired after a post is persisted to the DB; Git sync runs after transaction
 * commit.
 */
public record PostGitSyncRequestedEvent(long postId, GitSyncTrigger trigger) {}
