package dev.vepo.contraponto.components.forms;

import jakarta.ws.rs.FormParam;

public record ScheduleRequest(@FormParam("postId") Long postId, @FormParam("scheduledAt") String scheduledAt) {}
