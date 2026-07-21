package dev.vepo.contraponto.components.forms;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostManagementService;
import dev.vepo.contraponto.post.PostScheduleService;
import dev.vepo.contraponto.shared.i18n.I18nDefaults;
import dev.vepo.contraponto.shared.i18n.I18nKeys;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.toast.Toast;
import dev.vepo.contraponto.user.LoggedUser;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Logged
@ApplicationScoped
@Path("/forms/write/schedule")
public class ScheduleEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance banner(Post post);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final PostManagementService postManagementService;
    private final PostScheduleService postScheduleService;
    private final LoggedUser loggedUser;

    @Inject
    public ScheduleEndpoint(PostManagementService postManagementService,
                            PostScheduleService postScheduleService,
                            LoggedUser loggedUser) {
        this.postManagementService = postManagementService;
        this.postScheduleService = postScheduleService;
        this.loggedUser = loggedUser;
    }

    private Response badRequest(String i18nKey, String defaultMessage) {
        return Toast.response(Response.Status.BAD_REQUEST)
                    .i18nKey(i18nKey, defaultMessage)
                    .type(Toast.Type.ERROR)
                    .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                    .build();
    }

    @POST
    @Path("cancel/{postId}")
    @Produces(MediaType.TEXT_HTML)
    @Transactional
    public Response cancel(@PathParam("postId") long postId) {
        if (!loggedUser.isAuthenticated()) {
            return forbidden();
        }
        try {
            Post post = postManagementService.requireOwnedPost(postId, loggedUser);
            postScheduleService.cancel(post);
            return Toast.ok()
                        .i18nKey(I18nKeys.TOAST_POST_SCHEDULE_CANCELLED, I18nDefaults.POST_SCHEDULE_CANCELLED)
                        .type(Toast.Type.SUCCESS)
                        .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                        .build();
        } catch (NotFoundException e) {
            return notFound();
        } catch (BadRequestException e) {
            return badRequest(I18nKeys.TOAST_POST_NOT_SCHEDULED, I18nDefaults.POST_NOT_SCHEDULED);
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

    private Optional<LocalDateTime> parseScheduledAt(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDateTime.ofInstant(Instant.parse(value), ZoneOffset.UTC));
        } catch (java.time.format.DateTimeParseException e) {
            return Optional.empty();
        }
    }

    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Transactional
    public Response schedule(@BeanParam ScheduleRequest request) {
        if (!loggedUser.isAuthenticated()) {
            return forbidden();
        }
        if (request.postId() == null) {
            return badRequest(I18nKeys.TOAST_POST_NOT_FOUND, I18nDefaults.POST_NOT_FOUND);
        }
        var scheduledAt = parseScheduledAt(request.scheduledAt());
        if (scheduledAt.isEmpty() || !postScheduleService.meetsMinimumLeadTime(scheduledAt.get())) {
            return badRequest(I18nKeys.TOAST_POST_SCHEDULE_INVALID_TIME, I18nDefaults.POST_SCHEDULE_INVALID_TIME);
        }
        try {
            Post post = postManagementService.requireOwnedPost(request.postId(), loggedUser);
            postScheduleService.schedule(post, scheduledAt.get());
            return Toast.ok()
                        .i18nKey(I18nKeys.TOAST_POST_SCHEDULED, I18nDefaults.POST_SCHEDULED)
                        .type(Toast.Type.SUCCESS)
                        .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                        .page(Templates.banner(post))
                        .build();
        } catch (NotFoundException e) {
            return notFound();
        } catch (BadRequestException e) {
            return badRequest(I18nKeys.TOAST_POST_ALREADY_PUBLISHED, I18nDefaults.POST_ALREADY_PUBLISHED);
        }
    }
}
