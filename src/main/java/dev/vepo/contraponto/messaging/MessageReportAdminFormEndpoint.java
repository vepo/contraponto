package dev.vepo.contraponto.messaging;

import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.i18n.I18nDefaults;
import dev.vepo.contraponto.shared.i18n.I18nKeys;
import dev.vepo.contraponto.shared.toast.Toast;
import dev.vepo.contraponto.user.LoggedUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

@Logged
@Path("/forms/administration/message-reports")
@ApplicationScoped
public class MessageReportAdminFormEndpoint {

    private final MessageReportService reportService;
    private final MessageThreadAccess threadAccess;
    private final LoggedUser loggedUser;

    @Inject
    public MessageReportAdminFormEndpoint(MessageReportService reportService,
                                          MessageThreadAccess threadAccess,
                                          LoggedUser loggedUser) {
        this.reportService = reportService;
        this.threadAccess = threadAccess;
        this.loggedUser = loggedUser;
    }

    @POST
    @Path("{reportId}/dismiss")
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    public Response dismiss(@PathParam("reportId") long reportId) {
        requireAdmin();
        try {
            reportService.dismiss(reportId, loggedUser.getId());
            return Response.seeOther(UriBuilder.fromPath("/administration/message-reports").build()).build();
        } catch (NotFoundException _) {
            return Toast.response(Response.Status.NOT_FOUND)
                        .i18nKey(I18nKeys.MESSAGING_REPORT_NOT_FOUND, I18nDefaults.MESSAGING_REPORT_NOT_FOUND)
                        .type(Toast.Type.ERROR)
                        .build();
        }
    }

    private void requireAdmin() {
        if (!threadAccess.canReviewReports(loggedUser)) {
            throw new NotFoundException("Not found.");
        }
    }

    @POST
    @Path("{reportId}/reviewed")
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    public Response reviewed(@PathParam("reportId") long reportId) {
        requireAdmin();
        try {
            reportService.markReviewed(reportId, loggedUser.getId());
            return Response.seeOther(UriBuilder.fromPath("/administration/message-reports").build()).build();
        } catch (NotFoundException _) {
            return Toast.response(Response.Status.NOT_FOUND)
                        .i18nKey(I18nKeys.MESSAGING_REPORT_NOT_FOUND, I18nDefaults.MESSAGING_REPORT_NOT_FOUND)
                        .type(Toast.Type.ERROR)
                        .build();
        }
    }
}
