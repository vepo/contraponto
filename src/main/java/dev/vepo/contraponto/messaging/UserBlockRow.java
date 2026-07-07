package dev.vepo.contraponto.messaging;

import java.time.LocalDateTime;

public record UserBlockRow(long blockId,
                           long blockedUserId,
                           String blockedUsername,
                           String blockedName,
                           String reason,
                           LocalDateTime blockedAt) {}
