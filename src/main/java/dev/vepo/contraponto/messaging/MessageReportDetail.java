package dev.vepo.contraponto.messaging;

import java.util.List;

public record MessageReportDetail(MessageReport report, List<ThreadMessage> messages) {}
