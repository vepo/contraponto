package dev.vepo.contraponto.postresponse;

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
@Path("/forms/post-responses/{responseId}")
public class PostResponseModerationEndpoint {

    private final PostResponseService responseService;
    private final NavigationHubService navigationHubService;
    private final LoggedUser loggedUser;

    @Inject
    public PostResponseModerationEndpoint(PostResponseService responseService,
                                          NavigationHubService navigationHubService,
                                          LoggedUser loggedUser) {
        this.responseService = responseService;
        this.navigationHubService = navigationHubService;
        this.loggedUser = loggedUser;
    }

    @POST
    @Path("approve")
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    public Response approve(@PathParam("responseId") long responseId, @QueryParam("from") String from) {
        return moderate(responseId, "approve", from);
    }

    private Response moderate(long responseId, String action, String from) {
        try {
            switch (action) {
                case "approve" -> responseService.approve(responseId, loggedUser.getId());
                case "reject" -> responseService.reject(responseId, loggedUser.getId());
                case "revoke" -> responseService.revoke(responseId, loggedUser.getId());
                default -> throw new IllegalArgumentException("Unknown action: %s".formatted(action));
            }
            var builder = Toast.ok()
                               .message("Response updated.")
                               .type(Toast.Type.SUCCESS)
                               .duration(Toast.TOAST_DEFAULT_DURATION_MS);
            if ("writing".equals(from)) {
                return builder.page(navigationHubService.shell(NavigationHub.WRITING, "highlights", 1)).build();
            }
            return builder.build();
        } catch (ForbiddenException e) {
            return Toast.response(Status.FORBIDDEN).message(e.getMessage()).type(Toast.Type.ERROR).build();
        } catch (NotFoundException _) {
            return Toast.response(Status.NOT_FOUND).message("Response not found.").type(Toast.Type.ERROR).build();
        }
    }

    @POST
    @Path("reject")
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    public Response reject(@PathParam("responseId") long responseId, @QueryParam("from") String from) {
        return moderate(responseId, "reject", from);
    }

    @POST
    @Path("revoke")
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    public Response revoke(@PathParam("responseId") long responseId, @QueryParam("from") String from) {
        return moderate(responseId, "revoke", from);
    }
}
