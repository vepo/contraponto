package dev.vepo.contraponto.highlight;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ReadingLibraryService {

    private final PostTextHighlightRepository highlightRepository;
    private final HighlightNoteRepository noteRepository;

    @Inject
    public ReadingLibraryService(PostTextHighlightRepository highlightRepository,
                                 HighlightNoteRepository noteRepository) {
        this.highlightRepository = highlightRepository;
        this.noteRepository = noteRepository;
    }

    public Page<HighlightLibraryEntry> findHighlightsPage(long userId, PageQuery query) {
        Page<HighlightLibraryRow> highlights = highlightRepository.findLibraryForUser(userId, query);
        List<Long> highlightIds = highlights.data().stream().map(HighlightLibraryRow::highlightId).toList();
        Map<Long, List<HighlightNoteCardView>> notesByHighlight = notesByHighlightForUser(userId, highlightIds);
        List<HighlightLibraryEntry> entries = highlights.data().stream()
                                                        .map(row -> new HighlightLibraryEntry(row,
                                                                                              notesByHighlight.getOrDefault(
                                                                                                                            row.highlightId(),
                                                                                                                            List.of())))
                                                        .toList();
        return new Page<>(entries, highlights.page(), highlights.limit(), highlights.total());
    }

    public Page<HighlightNoteLibraryRow> findNotesPage(long userId, PageQuery query) {
        return noteRepository.findLibraryForUser(userId, query);
    }

    private Map<Long, List<HighlightNoteCardView>> notesByHighlightForUser(long userId, List<Long> highlightIds) {
        if (highlightIds.isEmpty()) {
            return Map.of();
        }
        List<HighlightNote> notes = noteRepository.findByUserAndHighlightIds(userId, highlightIds);
        return notes.stream()
                    .collect(Collectors.groupingBy(n -> n.getHighlight().getId(),
                                                   Collectors.mapping(this::toNoteCard, Collectors.toList())));
    }

    private HighlightNoteCardView toNoteCard(HighlightNote note) {
        return new HighlightNoteCardView(note.getId(),
                                         note.getHighlight().getId(),
                                         note.getHighlight().getPassage(),
                                         note.getBody(),
                                         note.getUser().getName(),
                                         note.getStatus(),
                                         note.isPublicNote(),
                                         note.getCreatedAt());
    }
}
