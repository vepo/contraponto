package dev.vepo.contraponto.highlight;

import java.time.LocalDateTime;

public record HighlightNoteCardView(long noteId,
                                    long highlightId,
                                    String passageExcerpt,
                                    String body,
                                    String ownerName,
                                    HighlightNoteStatus status,
                                    boolean publicNote,
                                    LocalDateTime createdAt) {

    public boolean approvedPublic() {
        return publicNote && status == HighlightNoteStatus.APPROVED;
    }
}
