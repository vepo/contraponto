package dev.vepo.contraponto.messaging;

import dev.vepo.contraponto.custompage.CustomPageRepository;
import dev.vepo.contraponto.custompage.Links;
import dev.vepo.contraponto.seo.SeoMetadata;
import dev.vepo.contraponto.seo.SeoService;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.i18n.I18nDefaults;
import dev.vepo.contraponto.shared.i18n.I18nKeys;
import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import dev.vepo.contraponto.shared.toast.Toast;
import dev.vepo.contraponto.user.LoggedUser;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

@Logged
@ApplicationScoped
public class MessageReportAdminEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance detail(MessageReportDetail detail,
                                                     String basePath,
                                                     SeoMetadata seo,
                                                     Links links,
                                                     LoggedUser user);

        public static native TemplateInstance panel(Page<MessageReportRow> reports, String basePath);

        private Templates() {
            throw new UnsupportedOperationException("Utility class");
        }
    }

    private final MessageReportRepository reportRepository;
    private final MessageReportService reportService;
    private final MessageThreadAccess threadAccess;
    private final CustomPageRepository customPageRepository;
    private final SeoService seoService;
    private final LoggedUser loggedUser;

    @Inject
    public MessageReportAdminEndpoint(MessageReportRepository reportRepository,
                                      MessageReportService reportService,
                                      MessageThreadAccess threadAccess,
                                      CustomPageRepository customPageRepository,
                                      SeoService seoService,
                                      LoggedUser loggedUser) {
        this.reportRepository = reportRepository;
        this.reportService = reportService;
        this.threadAccess = threadAccess;
        this.customPageRepository = customPageRepository;
        this.seoService = seoService;
        this.loggedUser = loggedUser;
    }

    @GET
    @Path("/administration/message-reports/{reportId}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance detail(@PathParam("reportId") long reportId) {
        requireAdmin();
        MessageReportDetail detail = reportService.loadDetail(reportId);
        return Templates.detail(detail,
                                "/administration/message-reports",
                                seoService.forPrivatePage("Denúncia de mensagem"),
                                customPageRepository.loadLinks(),
                                loggedUser);
    }

    @POST
    @Path("/forms/administration/message-reports/{reportId}/dismiss")
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

    public TemplateInstance renderHubPanel(int page, String basePath) {
        requireAdmin();
        Page<MessageReportRow> reports = reportRepository.findPendingPage(PageQuery.forGrid(20, page));
        return Templates.panel(reports, basePath);
    }

    private void requireAdmin() {
        if (!threadAccess.canReviewReports(loggedUser)) {
            throw new NotFoundException("Not found.");
        }
    }

    @POST
    @Path("/forms/administration/message-reports/{reportId}/reviewed")
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
