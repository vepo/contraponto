package dev.vepo.contraponto.highlight;

import java.time.LocalDateTime;

public record HighlightLibraryRow(long highlightId,
                                  long postId,
                                  String postTitle,
                                  String postSlug,
                                  String blogSlug,
                                  String authorUsername,
                                  String passage,
                                  LocalDateTime createdAt) {}
