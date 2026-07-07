package dev.vepo.contraponto.messaging;

public record UserUnblockedEvent(long blockerUserId, long blockedUserId) {}
