package dev.vepo.contraponto.image;

public record ImageUsageRow(long resourceId, String title, ImageRole role, ImageUsageKind kind) {}
