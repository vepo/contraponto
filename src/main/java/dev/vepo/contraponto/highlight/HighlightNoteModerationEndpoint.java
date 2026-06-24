package dev.vepo.contraponto.highlight;

import dev.vepo.contraponto.navigation.NavigationHub;
import dev.vepo.contraponto.navigation.NavigationHubService;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.user.LoggedUser;
import dev.vepo.contraponto.shared.toast.Toast;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Logged
@ApplicationScoped
@Path("/forms/highlight-notes/{noteId}")
public class HighlightNoteModerationEndpoint {

    private final HighlightNoteService noteService;
    private final NavigationHubService navigationHubService;
    private final LoggedUser loggedUser;

    @Inject
    public HighlightNoteModerationEndpoint(HighlightNoteService noteService,
                                           NavigationHubService navigationHubService,
                                           LoggedUser loggedUser) {
        this.noteService = noteService;
        this.navigationHubService = navigationHubService;
        this.loggedUser = loggedUser;
    }

    @POST
    @Path("approve")
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    public Response approve(@PathParam("noteId") long noteId, @QueryParam("from") String from) {
        return moderate(noteId, true, from);
    }

    private Response moderate(long noteId, boolean approve, String from) {
        try {
            if (approve) {
                noteService.approve(noteId, loggedUser.getId());
            } else {
                noteService.reject(noteId, loggedUser.getId());
            }
            var builder = Toast.ok()
                               .message(approve ? "Note approved." : "Note rejected.")
                               .type(Toast.Type.SUCCESS)
                               .duration(Toast.TOAST_DEFAULT_DURATION_MS);
            if ("writing".equals(from)) {
                return builder.page(navigationHubService.shell(NavigationHub.WRITING, "highlights", 1)).build();
            }
            return builder.build();
        } catch (ForbiddenException e) {
            return Toast.response(Status.FORBIDDEN).message(e.getMessage()).type(Toast.Type.ERROR).build();
        }
    }

    @POST
    @Path("reject")
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    public Response reject(@PathParam("noteId") long noteId, @QueryParam("from") String from) {
        return moderate(noteId, false, from);
    }
}
