package dev.vepo.contraponto.image;

import java.time.LocalDateTime;
import java.util.List;

public record ImageControlRow(String uuid,
                              String url,
                              String filename,
                              String displayFilename,
                              String altText,
                              LocalDateTime createdAt,
                              List<ImageUsageView> usages) {}
