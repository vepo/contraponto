package dev.vepo.contraponto.highlight;

import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.shared.i18n.I18nDefaults;
import dev.vepo.contraponto.shared.i18n.I18nKeys;
import dev.vepo.contraponto.shared.toast.Toast;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.FormParam;
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

    private final HighlightNoteService noteService;
    private final LoggedUser loggedUser;

    @Inject
    public HighlightNoteFormEndpoint(HighlightNoteService noteService, LoggedUser loggedUser) {
        this.noteService = noteService;
        this.loggedUser = loggedUser;
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
