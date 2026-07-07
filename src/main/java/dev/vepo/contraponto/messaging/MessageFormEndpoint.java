package dev.vepo.contraponto.messaging;

import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.i18n.I18nDefaults;
import dev.vepo.contraponto.shared.i18n.I18nKeys;
import dev.vepo.contraponto.shared.toast.Toast;
import dev.vepo.contraponto.user.LoggedUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

@Logged
@ApplicationScoped
@Path("/forms/messages")
public class MessageFormEndpoint {

    private final MessageComposeService composeService;
    private final MessageThreadService threadService;
    private final UserBlockService blockService;
    private final LoggedUser loggedUser;

    @Inject
    public MessageFormEndpoint(MessageComposeService composeService,
                               MessageThreadService threadService,
                               UserBlockService blockService,
                               LoggedUser loggedUser) {
        this.composeService = composeService;
        this.threadService = threadService;
        this.blockService = blockService;
        this.loggedUser = loggedUser;
    }

    @POST
    @Path("blocks/{blockedUserId}")
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    public Response block(@PathParam("blockedUserId") long blockedUserId,
                          @QueryParam("returnUrl") String returnUrl) {
        try {
            blockService.block(loggedUser.getId(), blockedUserId);
            String target = returnUrl != null && !returnUrl.isBlank() ? returnUrl : MessageThreadPaths.blocked();
            return Response.seeOther(UriBuilder.fromPath(target).build()).build();
        } catch (NotFoundException _) {
            return Toast.response(Response.Status.NOT_FOUND)
                        .i18nKey(I18nKeys.MESSAGING_USER_NOT_FOUND, I18nDefaults.MESSAGING_USER_NOT_FOUND)
                        .type(Toast.Type.ERROR)
                        .build();
        }
    }

    @POST
    @Path("threads/{threadId}/close")
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    public Response close(@PathParam("threadId") long threadId) {
        try {
            threadService.close(threadId, loggedUser.getId());
            return Response.seeOther(UriBuilder.fromPath(MessageThreadPaths.thread(threadId)).build()).build();
        } catch (NotFoundException _) {
            return Toast.response(Response.Status.NOT_FOUND)
                        .i18nKey(I18nKeys.MESSAGING_THREAD_NOT_FOUND, I18nDefaults.MESSAGING_THREAD_NOT_FOUND)
                        .type(Toast.Type.ERROR)
                        .build();
        }
    }

    @POST
    @Path("compose")
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    public Response compose(@FormParam("to") String to,
                            @FormParam("title") String title,
                            @FormParam("body") String body) {
        try {
            MessageThread thread = composeService.compose(loggedUser.getId(), to, title, body);
            return Response.seeOther(UriBuilder.fromPath(MessageThreadPaths.thread(thread.getId())).build()).build();
        } catch (NotFoundException _) {
            return Toast.response(Response.Status.NOT_FOUND)
                        .i18nKey(I18nKeys.MESSAGING_USER_NOT_FOUND, I18nDefaults.MESSAGING_USER_NOT_FOUND)
                        .type(Toast.Type.ERROR)
                        .build();
        }
    }

    @POST
    @Path("threads/{threadId}/flag")
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    public Response flag(@PathParam("threadId") long threadId) {
        try {
            threadService.flag(threadId, loggedUser.getId());
            return Response.seeOther(UriBuilder.fromPath(MessageThreadPaths.thread(threadId)).build()).build();
        } catch (NotFoundException _) {
            return Toast.response(Response.Status.NOT_FOUND)
                        .i18nKey(I18nKeys.MESSAGING_THREAD_NOT_FOUND, I18nDefaults.MESSAGING_THREAD_NOT_FOUND)
                        .type(Toast.Type.ERROR)
                        .build();
        }
    }

    @POST
    @Path("threads/{threadId}/reply")
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    public Response reply(@PathParam("threadId") long threadId, @FormParam("body") String body) {
        try {
            threadService.reply(threadId, loggedUser.getId(), body);
            return Response.seeOther(UriBuilder.fromPath(MessageThreadPaths.thread(threadId)).build()).build();
        } catch (NotFoundException _) {
            return Toast.response(Response.Status.NOT_FOUND)
                        .i18nKey(I18nKeys.MESSAGING_THREAD_NOT_FOUND, I18nDefaults.MESSAGING_THREAD_NOT_FOUND)
                        .type(Toast.Type.ERROR)
                        .build();
        }
    }

    @POST
    @Path("blocks/{blockedUserId}/unblock")
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    public Response unblock(@PathParam("blockedUserId") long blockedUserId,
                            @FormParam("returnUrl") String returnUrl) {
        try {
            blockService.unblock(loggedUser.getId(), blockedUserId);
            String target = returnUrl != null && !returnUrl.isBlank() ? returnUrl : MessageThreadPaths.blocked();
            return Response.seeOther(UriBuilder.fromPath(target).build()).build();
        } catch (NotFoundException _) {
            return Toast.response(Response.Status.NOT_FOUND)
                        .i18nKey(I18nKeys.MESSAGING_USER_NOT_FOUND, I18nDefaults.MESSAGING_USER_NOT_FOUND)
                        .type(Toast.Type.ERROR)
                        .build();
        }
    }
}
