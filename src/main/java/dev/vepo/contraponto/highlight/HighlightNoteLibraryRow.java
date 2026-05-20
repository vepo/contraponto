package dev.vepo.contraponto.highlight;

import java.time.LocalDateTime;

public record HighlightNoteLibraryRow(long noteId,
                                      long highlightId,
                                      long postId,
                                      String postTitle,
                                      String postSlug,
                                      String authorUsername,
                                      String blogSlug,
                                      String passageExcerpt,
                                      String body,
                                      String ownerName,
                                      HighlightNoteStatus status,
                                      boolean publicNote,
                                      LocalDateTime createdAt) {

    public HighlightNoteCardView noteCard() {
        return new HighlightNoteCardView(noteId,
                                         highlightId,
                                         passageExcerpt,
                                         body,
                                         ownerName,
                                         status,
                                         publicNote,
                                         createdAt);
    }
}
