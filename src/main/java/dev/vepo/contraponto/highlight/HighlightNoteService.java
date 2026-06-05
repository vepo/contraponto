package dev.vepo.contraponto.highlight;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import dev.vepo.contraponto.notification.NotificationService;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.user.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class HighlightNoteService {

    private final HighlightNoteRepository noteRepository;
    private final PostTextHighlightRepository highlightRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final int maxNoteLength;
    private final int maxNotesPerHighlight;
    private final int maxPendingPublicPerPost;

    @Inject
    public HighlightNoteService(HighlightNoteRepository noteRepository,
                                PostTextHighlightRepository highlightRepository,
                                UserRepository userRepository,
                                NotificationService notificationService,
                                @ConfigProperty(name = "contraponto.highlight.max-note-length", defaultValue = "1000") int maxNoteLength,
                                @ConfigProperty(name = "contraponto.highlight.max-notes-per-highlight", defaultValue = "5") int maxNotesPerHighlight,
                                @ConfigProperty(name = "contraponto.highlight.max-pending-public-notes-per-post", defaultValue = "3") int maxPendingPublicPerPost) {
        this.noteRepository = noteRepository;
        this.highlightRepository = highlightRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.maxNoteLength = maxNoteLength;
        this.maxNotesPerHighlight = maxNotesPerHighlight;
        this.maxPendingPublicPerPost = maxPendingPublicPerPost;
    }

    @Transactional
    public void approve(long noteId, long ownerUserId) {
        HighlightNote note = loadForModeration(noteId, ownerUserId);
        if (!note.isPublicNote() || note.getStatus() != HighlightNoteStatus.PENDING) {
            throw new BadRequestException("Note is not pending approval.");
        }
        note.setStatus(HighlightNoteStatus.APPROVED);
        noteRepository.save(note);
    }

    private HighlightNote loadForModeration(long noteId, long ownerUserId) {
        HighlightNote note = noteRepository.findById(noteId).orElseThrow(NotFoundException::new);
        if (!note.getHighlight().getPost().getAuthor().getId().equals(ownerUserId)) {
            throw new ForbiddenException("Only the post owner can moderate highlight notes.");
        }
        return note;
    }

    @Transactional
    public void reject(long noteId, long ownerUserId) {
        HighlightNote note = loadForModeration(noteId, ownerUserId);
        note.setStatus(HighlightNoteStatus.REJECTED);
        noteRepository.save(note);
    }

    @Transactional
    public void remove(long noteId, long userId) {
        HighlightNote note = noteRepository.findById(noteId).orElseThrow(NotFoundException::new);
        if (!note.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You can only remove your own notes.");
        }
        noteRepository.delete(note);
    }

    @Transactional
    public HighlightNote saveNote(long highlightId, long userId, String body, boolean makePublic) {
        PostTextHighlight highlight = highlightRepository.findById(highlightId)
                                                         .orElseThrow(NotFoundException::new);
        if (!highlight.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You can only add notes to your own highlights.");
        }
        User user = userRepository.findById(userId).orElseThrow(NotFoundException::new);
        String trimmed = validateBody(body);

        if (noteRepository.countByHighlightAndUser(highlightId, userId) >= maxNotesPerHighlight) {
            throw new BadRequestException("Maximum notes per highlight reached.");
        }
        if (makePublic && noteRepository.countPendingPublicByUserAndPost(userId, highlight.getPost().getId()) >= maxPendingPublicPerPost) {
            throw new BadRequestException("Maximum pending public notes per post reached.");
        }

        HighlightNote note = new HighlightNote();
        note.setHighlight(highlight);
        note.setUser(user);
        note.setBody(trimmed);
        note.setPublicNote(makePublic);
        note.setStatus(makePublic ? HighlightNoteStatus.PENDING : HighlightNoteStatus.PRIVATE);
        noteRepository.save(note);

        if (makePublic) {
            notificationService.notifyPublicHighlightNote(highlight.getPost().getAuthor(),
                                                          highlight.getPost(),
                                                          user);
        }
        return note;
    }

    private String validateBody(String body) {
        if (body == null) {
            throw new BadRequestException("Note body is required.");
        }
        String trimmed = body.trim();
        if (trimmed.isEmpty()) {
            throw new BadRequestException("Note body is required.");
        }
        if (trimmed.length() > maxNoteLength) {
            throw new BadRequestException("Note must be at most %s characters.".formatted(maxNoteLength));
        }
        return trimmed;
    }
}
