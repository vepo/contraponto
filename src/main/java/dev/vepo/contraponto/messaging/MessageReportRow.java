package dev.vepo.contraponto.messaging;

import java.time.LocalDateTime;

public record MessageReportRow(long reportId,
                               long threadId,
                               String threadTitle,
                               String reporterUsername,
                               String reporterName,
                               MessageReportStatus status,
                               LocalDateTime reportedAt) {}
