package dev.vepo.contraponto.highlight;

import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.shared.i18n.I18nDefaults;
import dev.vepo.contraponto.shared.i18n.I18nKeys;
import dev.vepo.contraponto.shared.toast.Toast;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Logged
@ApplicationScoped
@Path("/forms/highlights/{highlightId}/notes")
public class HighlightNoteFormEndpoint {

    @CheckedTemplate
    public static class Templates {
        static native TemplateInstance highlightNoteModal(long highlightId, String passageExcerpt);

        private Templates() {
            throw new UnsupportedOperationException("Utility class");
        }
    }

    private final HighlightNoteService noteService;
    private final PostTextHighlightRepository highlightRepository;
    private final LoggedUser loggedUser;

    @Inject
    public HighlightNoteFormEndpoint(HighlightNoteService noteService,
                                     PostTextHighlightRepository highlightRepository,
                                     LoggedUser loggedUser) {
        this.noteService = noteService;
        this.highlightRepository = highlightRepository;
        this.loggedUser = loggedUser;
    }

    private String excerpt(String passage) {
        if (passage == null) {
            return "";
        }
        String trimmed = passage.trim();
        if (trimmed.length() <= 120) {
            return trimmed;
        }
        return trimmed.substring(0, 119) + "…";
    }

    private PostTextHighlight loadOwnedHighlight(long highlightId) {
        PostTextHighlight highlight = highlightRepository.findById(highlightId).orElseThrow(NotFoundException::new);
        if (!highlight.getUser().getId().equals(loggedUser.getId())) {
            throw new ForbiddenException("You can only add notes to your own highlights.");
        }
        return highlight;
    }

    @GET
    @Path("modal")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance modal(@PathParam("highlightId") long highlightId) {
        PostTextHighlight highlight = loadOwnedHighlight(highlightId);
        return Templates.highlightNoteModal(highlightId, excerpt(highlight.getPassage()));
    }

    @POST
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    public Response save(@PathParam("highlightId") long highlightId,
                         @FormParam("body") String body,
                         @FormParam("makePublic") boolean makePublic) {
        try {
            HighlightNote note = noteService.saveNote(highlightId, loggedUser.getId(), body, makePublic);
            if (note.isPublicNote()) {
                return Toast.ok()
                            .i18nKey(I18nKeys.TOAST_HIGHLIGHT_NOTE_PENDING, I18nDefaults.HIGHLIGHT_NOTE_PENDING)
                            .type(Toast.Type.SUCCESS)
                            .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                            .build();
            }
            return Toast.ok()
                        .message("Note saved.")
                        .type(Toast.Type.SUCCESS)
                        .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                        .build();
        } catch (BadRequestException e) {
            return Toast.response(Status.BAD_REQUEST).message(e.getMessage()).type(Toast.Type.ERROR).build();
        }
    }
}
