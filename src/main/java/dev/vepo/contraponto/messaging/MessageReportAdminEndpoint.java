package dev.vepo.contraponto.messaging;

import dev.vepo.contraponto.navigation.BreadcrumbService;
import dev.vepo.contraponto.navigation.NavigationHub;
import dev.vepo.contraponto.navigation.NavigationHubService;
import dev.vepo.contraponto.seo.SeoService;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import dev.vepo.contraponto.user.LoggedUser;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.RawString;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Logged
@Path("/administration/message-reports")
@ApplicationScoped
public class MessageReportAdminEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance detailPanel(MessageReportDetail detail);

        public static native TemplateInstance panel(Page<MessageReportRow> reports, String basePath);

        private Templates() {
            throw new UnsupportedOperationException("Utility class");
        }
    }

    private final MessageReportRepository reportRepository;
    private final MessageReportService reportService;
    private final MessageThreadAccess threadAccess;
    private final BreadcrumbService breadcrumbService;
    private final NavigationHubService hubService;
    private final SeoService seoService;
    private final LoggedUser loggedUser;

    @Inject
    public MessageReportAdminEndpoint(MessageReportRepository reportRepository,
                                      MessageReportService reportService,
                                      MessageThreadAccess threadAccess,
                                      BreadcrumbService breadcrumbService,
                                      NavigationHubService hubService,
                                      SeoService seoService,
                                      LoggedUser loggedUser) {
        this.reportRepository = reportRepository;
        this.reportService = reportService;
        this.threadAccess = threadAccess;
        this.breadcrumbService = breadcrumbService;
        this.hubService = hubService;
        this.seoService = seoService;
        this.loggedUser = loggedUser;
    }

    @GET
    @Path("{reportId}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance detail(@PathParam("reportId") long reportId) {
        requireAdmin();
        MessageReportDetail detail = reportService.loadDetail(reportId);
        var breadcrumb = breadcrumbService.forMessageReportDetail(detail.report());
        return hubService.shellWithCustomPanel(NavigationHub.ADMINISTRATION,
                                               "message-reports",
                                               breadcrumb,
                                               new RawString(Templates.detailPanel(detail).render()),
                                               seoService.forPrivatePage(detail.report().getThread().getTitle()));
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list(@QueryParam("page") @DefaultValue("1") int page) {
        requireAdmin();
        return hubService.shell(NavigationHub.ADMINISTRATION, "message-reports", page);
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
}
