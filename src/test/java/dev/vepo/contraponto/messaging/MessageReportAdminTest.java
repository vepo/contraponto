package dev.vepo.contraponto.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebPlatformTest;
import dev.vepo.contraponto.user.Role;
import dev.vepo.contraponto.user.User;

@WebPlatformTest
class MessageReportAdminTest {

    private static final String PASSWORD = "password123";

    private User admin;
    private User alice;
    private User bob;
    private MessageReport report;

    @Test
    void adminDismissesReportFromDetail(App app) {
        app.login(admin)
           .goToPath("/administration/message-reports")
           .assertPageSourceContains("Web report thread")
           .openMessageReportDetail(report.getId())
           .assertSingleSiteHeader()
           .assertSingleMainElement()
           .assertPageSourceContains("hub-layout")
           .dismissMessageReport()
           .assertSingleSiteHeader()
           .assertSingleMainElement()
           .assertPageSourceContains("hub-layout");

        assertThat(Given.transaction(() -> Given.inject(MessageReportRepository.class)
                                                .findById(report.getId())
                                                .orElseThrow()
                                                .getStatus())).isEqualTo(MessageReportStatus.DISMISSED);
    }

    @Test
    void adminSeesPendingReport(App app) {
        app.login(admin)
           .goToPath("/administration/message-reports")
           .assertHubNavTabsVisible()
           .assertPageSourceContains("Web report thread");
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        admin = Given.user()
                     .withUsername("msgadmin")
                     .withEmail("msgadmin@test.com")
                     .withName("Msg Admin")
                     .withPassword(PASSWORD)
                     .withRole(Role.ADMIN)
                     .persist();
        alice = Given.user()
                     .withUsername("report-alice2")
                     .withEmail("report-alice2@test.com")
                     .withName("Report Alice")
                     .withPassword(PASSWORD)
                     .persist();
        bob = Given.user()
                   .withUsername("report-bob2")
                   .withEmail("report-bob2@test.com")
                   .withName("Report Bob")
                   .withPassword(PASSWORD)
                   .persist();
        Given.transaction(() -> {
            var composeService = Given.inject(MessageComposeService.class);
            var threadService = Given.inject(MessageThreadService.class);
            var thread = composeService.compose(alice.getId(), bob.getUsername(), "Web report thread", "Body");
            report = threadService.flag(thread.getId(), bob.getId());
        });
    }
}
