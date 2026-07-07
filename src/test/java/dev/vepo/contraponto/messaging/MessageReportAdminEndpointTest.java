package dev.vepo.contraponto.messaging;

import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.shared.TestHttp;
import dev.vepo.contraponto.user.Role;
import dev.vepo.contraponto.user.User;

@QuarkusIntegrationTest
class MessageReportAdminEndpointTest {

    private static final String PASSWORD = "password123";

    private User admin;
    private MessageReport report;

    @Test
    void detailReturnsReportPanelForAdmin() {
        TestHttp.authenticated(admin)
                .get("/administration/message-reports")
                .then()
                .statusCode(200)
                .body(containsString("HTTP report thread"));

        TestHttp.authenticated(admin)
                .header("HX-Request", "true")
                .get("/administration/message-reports/" + report.getId())
                .then()
                .statusCode(200)
                .body(containsString("HTTP report thread"))
                .body(containsString("administration.messageReports.dismiss"));
    }

    @Test
    void loadDetailFindsReportInService() {
        var detail = Given.transaction(() -> Given.inject(MessageReportService.class).loadDetail(report.getId()));
        org.assertj.core.api.Assertions.assertThat(detail.report().getThread().getTitle()).isEqualTo("HTTP report thread");
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        admin = Given.user()
                     .withUsername("rptadmin")
                     .withEmail("rptadmin@test.com")
                     .withName("Report Admin")
                     .withPassword(PASSWORD)
                     .withRole(Role.ADMIN)
                     .persist();
        var alice = Given.user()
                         .withUsername("rpt-alice")
                         .withEmail("rpt-alice@test.com")
                         .withName("Report Alice")
                         .withPassword(PASSWORD)
                         .persist();
        var bob = Given.user()
                       .withUsername("rpt-bob")
                       .withEmail("rpt-bob@test.com")
                       .withName("Report Bob")
                       .withPassword(PASSWORD)
                       .persist();
        report = Given.transaction(() -> {
            var composeService = Given.inject(MessageComposeService.class);
            var threadService = Given.inject(MessageThreadService.class);
            var thread = composeService.compose(alice.getId(), bob.getUsername(), "HTTP report thread", "Body");
            return threadService.flag(thread.getId(), bob.getId());
        });
    }
}
