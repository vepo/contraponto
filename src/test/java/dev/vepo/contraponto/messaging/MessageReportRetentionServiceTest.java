package dev.vepo.contraponto.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.shared.TestTimes;
import dev.vepo.contraponto.user.User;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@QuarkusIntegrationTest
class MessageReportRetentionServiceTest {

    private static final String PASSWORD = "password123";

    @Inject
    MessageReportRetentionService retentionService;

    @Inject
    MessageComposeService composeService;

    @Inject
    MessageThreadService threadService;

    @Inject
    MessageReportRepository reportRepository;

    @Inject
    EntityManager entityManager;

    private User alice;
    private User bob;

    @Transactional
    void backdateReport(long reportId) {
        entityManager.createNativeQuery("""
                                        UPDATE tb_message_reports
                                        SET created_at = :createdAt
                                        WHERE id = :id
                                        """)
                     .setParameter("createdAt", TestTimes.REFERENCE.minusDays(91))
                     .setParameter("id", reportId)
                     .executeUpdate();
    }

    @Test
    void purgeExpired_deletesReportsOlderThan90Days() {
        var thread = composeService.compose(alice.getId(), bob.getUsername(), "Old report", "Body");
        var report = threadService.flag(thread.getId(), bob.getId());
        backdateReport(report.getId());

        int deleted = retentionService.purgeExpired();

        assertThat(deleted).isEqualTo(1);
        entityManager.clear();
        assertThat(reportRepository.findById(report.getId())).isEmpty();
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        alice = Given.user()
                     .withUsername("report-alice")
                     .withEmail("report-alice@test.com")
                     .withName("Report Alice")
                     .withPassword(PASSWORD)
                     .persist();
        bob = Given.user()
                   .withUsername("report-bob")
                   .withEmail("report-bob@test.com")
                   .withName("Report Bob")
                   .withPassword(PASSWORD)
                   .persist();
    }
}
