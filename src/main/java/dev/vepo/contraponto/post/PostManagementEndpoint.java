package dev.vepo.contraponto.post;

import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.user.LoggedUser;
import dev.vepo.contraponto.shared.i18n.I18nDefaults;
import dev.vepo.contraponto.shared.i18n.I18nKeys;
import dev.vepo.contraponto.shared.toast.Toast;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Logged
@ApplicationScoped
@Path("/forms/posts/{postId}")
public class PostManagementEndpoint {

    private final PostManagementService postManagementService;
    private final LoggedUser loggedUser;

    @Inject
    public PostManagementEndpoint(PostManagementService postManagementService, LoggedUser loggedUser) {
        this.postManagementService = postManagementService;
        this.loggedUser = loggedUser;
    }

    private Response badRequest(String i18nKey, String defaultMessage) {
        return Toast.response(Response.Status.BAD_REQUEST)
                    .i18nKey(i18nKey, defaultMessage)
                    .type(Toast.Type.ERROR)
                    .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                    .build();
    }

    @DELETE
    @Produces(MediaType.TEXT_HTML)
    public Response delete(@PathParam("postId") long postId) {
        if (!loggedUser.isAuthenticated()) {
            return forbidden();
        }
        try {
            postManagementService.delete(postId, loggedUser);
            return Toast.ok()
                        .i18nKey(I18nKeys.TOAST_POST_DELETED, I18nDefaults.POST_DELETED)
                        .type(Toast.Type.SUCCESS)
                        .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                        .build();
        } catch (NotFoundException e) {
            return notFound();
        } catch (BadRequestException e) {
            return badRequest(I18nKeys.TOAST_POST_DELETE_PUBLISHED, I18nDefaults.POST_DELETE_PUBLISHED);
        }
    }

    private Response forbidden() {
        return Toast.response(Response.Status.FORBIDDEN)
                    .i18nKey(I18nKeys.TOAST_POST_FORBIDDEN, I18nDefaults.POST_FORBIDDEN)
                    .type(Toast.Type.ERROR)
                    .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                    .build();
    }

    private Response notFound() {
        return Toast.response(Response.Status.NOT_FOUND)
                    .i18nKey(I18nKeys.TOAST_POST_NOT_FOUND, I18nDefaults.POST_NOT_FOUND)
                    .type(Toast.Type.ERROR)
                    .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                    .build();
    }

    @POST
    @Path("unpublish")
    @Produces(MediaType.TEXT_HTML)
    public Response unpublish(@PathParam("postId") long postId) {
        if (!loggedUser.isAuthenticated()) {
            return forbidden();
        }
        try {
            postManagementService.unpublish(postId, loggedUser);
            return Toast.ok()
                        .i18nKey(I18nKeys.TOAST_POST_UNPUBLISHED, I18nDefaults.POST_UNPUBLISHED)
                        .type(Toast.Type.SUCCESS)
                        .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                        .build();
        } catch (NotFoundException e) {
            return notFound();
        } catch (BadRequestException e) {
            return badRequest(I18nKeys.TOAST_POST_ALREADY_DRAFT, I18nDefaults.POST_ALREADY_DRAFT);
        }
    }
}
