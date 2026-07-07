package dev.vepo.contraponto.messaging;

public record ThreadMessagePostedEvent(long threadId, long messageId, long authorUserId, long recipientUserId) {}
