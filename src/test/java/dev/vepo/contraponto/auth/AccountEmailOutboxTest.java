package dev.vepo.contraponto.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.shared.TestTimes;
import io.quarkus.mailer.MockMailbox;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class AccountEmailOutboxTest {

    @Inject
    MockMailbox mailbox;

    @Inject
    AccountEmailOutboxRepository outboxRepository;

    @Inject
    AccountEmailOutboxService outboxService;

    @Inject
    ObjectMapper objectMapper;

    @BeforeEach
    void clearState() {
        mailbox.clear();
        Given.cleanup();
    }

    @Test
    void queuedAccountActivationEmailIsDeliveredOnRetry() throws Exception {
        String recipient = "queued-activation@example.com";
        String activateUrl = "http://localhost:8080/account/activate?token=test-token";
        String reportUrl = "http://localhost:8080/account/report-signup?token=test-token";
        var event = new AccountActivationEvent(activateUrl, 48, "contraponto", "contraponto");
        var copy = new AccountEmailCopy("Activate your contraponto account",
                                        "Activate your account",
                                        "Hello,",
                                        "Thanks for signing up on contraponto.",
                                        "Activate account",
                                        activateUrl,
                                        "This link expires in 48 hours.",
                                        "You received this email because you signed up on contraponto.",
                                        "Security",
                                        reportUrl,
                                        "Report unauthorized signup");
        var payload = new AccountActivationOutboxPayload("http://localhost:8080", "en", event, copy);

        outboxRepository.persist(AccountEmailOutbox.pending(AccountEmailKind.ACCOUNT_ACTIVATION,
                                                            recipient,
                                                            copy.subject(),
                                                            objectMapper.writeValueAsString(payload),
                                                            "SMTP unavailable"));

        assertThat(outboxRepository.findDue(TestTimes.FAR_FUTURE, 10)).hasSize(1);

        outboxService.processDue();

        var messages = mailbox.getMessagesSentTo(recipient);
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getSubject()).isEqualTo(copy.subject());
        assertThat(messages.get(0).getHtml()).contains(activateUrl);
        assertThat(outboxRepository.findDue(TestTimes.FAR_FUTURE, 10)).isEmpty();
    }
}
