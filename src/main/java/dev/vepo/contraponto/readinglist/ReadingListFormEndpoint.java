package dev.vepo.contraponto.readinglist;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.user.LoggedUser;
import dev.vepo.contraponto.shared.i18n.I18nDefaults;
import dev.vepo.contraponto.shared.i18n.I18nKeys;
import dev.vepo.contraponto.shared.toast.Toast;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.container.ContainerRequestContext;

@Logged
@ApplicationScoped
@Path("/forms/reading-list/{itemId}")
public class ReadingListFormEndpoint {

    @FunctionalInterface
    private interface PostSupplier {
        Post get();
    }

    private final ReadingListService readingListService;
    private final ReadingListMutationResponse mutationResponse;

    private final LoggedUser loggedUser;

    @Inject
    public ReadingListFormEndpoint(ReadingListService readingListService,
                                   ReadingListMutationResponse mutationResponse,
                                   LoggedUser loggedUser) {
        this.readingListService = readingListService;
        this.mutationResponse = mutationResponse;
        this.loggedUser = loggedUser;
    }

    @POST
    @Path("read")
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    public Response markRead(@PathParam("itemId") long itemId,
                             @QueryParam("tab") String tab,
                             @QueryParam("page") @DefaultValue("1") int page,
                             @Context ContainerRequestContext requestContext) {
        return mutate(itemId,
                      tab,
                      page,
                      requestContext,
                      I18nKeys.TOAST_READING_LIST_MARKED_READ,
                      I18nDefaults.READING_LIST_MARKED_READ,
                      () -> readingListService.markRead(itemId, loggedUser.getId()).getPost());
    }

    @POST
    @Path("unread")
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    public Response markUnread(@PathParam("itemId") long itemId,
                               @QueryParam("tab") String tab,
                               @QueryParam("page") @DefaultValue("1") int page,
                               @Context ContainerRequestContext requestContext) {
        return mutate(itemId,
                      tab,
                      page,
                      requestContext,
                      I18nKeys.TOAST_READING_LIST_MARKED_UNREAD,
                      I18nDefaults.READING_LIST_MARKED_UNREAD,
                      () -> readingListService.markUnread(itemId, loggedUser.getId()).getPost());
    }

    private Response mutate(long itemId,
                            String tab,
                            int page,
                            ContainerRequestContext requestContext,
                            String toastKey,
                            String toastDefault,
                            PostSupplier postSupplier) {
        try {
            Post post = postSupplier.get();
            var toast = Toast.ok()
                             .i18nKey(toastKey, toastDefault)
                             .type(Toast.Type.SUCCESS)
                             .duration(Toast.TOAST_DEFAULT_DURATION_MS);
            return mutationResponse.build(toast, post, tab, page, requestContext);
        } catch (NotFoundException _) {
            return Toast.response(Status.NOT_FOUND)
                        .i18nKey(I18nKeys.TOAST_POST_NOT_FOUND, I18nDefaults.POST_NOT_FOUND)
                        .type(Toast.Type.ERROR)
                        .build();
        }
    }

    @DELETE
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    public Response remove(@PathParam("itemId") long itemId,
                           @QueryParam("tab") String tab,
                           @QueryParam("page") @DefaultValue("1") int page,
                           @Context ContainerRequestContext requestContext) {
        return mutate(itemId,
                      tab,
                      page,
                      requestContext,
                      I18nKeys.TOAST_READING_LIST_REMOVED,
                      I18nDefaults.READING_LIST_REMOVED,
                      () -> readingListService.remove(itemId, loggedUser.getId()));
    }
}
