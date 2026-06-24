package dev.vepo.contraponto.notification;

public record PostUnpublishedEvent(long postId, long blogId, long authorUserId) {}
