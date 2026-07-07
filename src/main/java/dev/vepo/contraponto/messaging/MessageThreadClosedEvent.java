package dev.vepo.contraponto.messaging;

public record MessageThreadClosedEvent(long threadId, long closedByUserId) {}
