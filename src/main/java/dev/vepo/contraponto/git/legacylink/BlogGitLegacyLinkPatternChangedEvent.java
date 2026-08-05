package dev.vepo.contraponto.git.legacylink;

/**
 * Fired after a blog's legacy link pattern is saved with an actual diff
 * (including first-time set); triggers an async legacy link reprocess.
 */
public record BlogGitLegacyLinkPatternChangedEvent(long blogId, String oldPattern, String newPattern) {}
