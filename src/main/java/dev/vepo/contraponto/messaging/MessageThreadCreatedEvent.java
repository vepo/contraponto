package dev.vepo.contraponto.messaging;

public record MessageThreadCreatedEvent(long threadId, long initiatorUserId, long recipientUserId) {}
