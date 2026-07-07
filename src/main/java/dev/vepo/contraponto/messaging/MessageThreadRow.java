package dev.vepo.contraponto.messaging;

import java.time.LocalDateTime;

public record MessageThreadRow(long threadId,
                               String title,
                               String otherUsername,
                               String otherName,
                               String preview,
                               LocalDateTime updatedAt,
                               boolean unread) {}
