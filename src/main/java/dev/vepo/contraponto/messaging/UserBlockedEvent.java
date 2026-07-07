package dev.vepo.contraponto.messaging;

public record UserBlockedEvent(long blockerUserId, long blockedUserId) {}
