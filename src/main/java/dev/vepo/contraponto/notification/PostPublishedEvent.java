package dev.vepo.contraponto.notification;

public record PostPublishedEvent(long postId, long publicationId, long blogId, long authorUserId) {}
