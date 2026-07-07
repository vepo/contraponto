package dev.vepo.contraponto.messaging;

public record MessageThreadFlaggedEvent(long threadId, long reportId, long reporterUserId) {}
