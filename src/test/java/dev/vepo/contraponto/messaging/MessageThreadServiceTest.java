package dev.vepo.contraponto.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.user.User;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

@QuarkusIntegrationTest
class MessageThreadServiceTest {

    private static final String PASSWORD = "password123";

    @Inject
    MessageComposeService composeService;

    @Inject
    MessageThreadService threadService;

    @Inject
    MessageReportRepository reportRepository;

    private User alice;
    private User bob;
    private MessageThread thread;

    @Test
    void close_preventsFurtherReplies() {
        threadService.close(thread.getId(), alice.getId());

        assertThatThrownBy(() -> threadService.reply(thread.getId(), bob.getId(), "Too late"))
                                                                                              .isInstanceOf(BadRequestException.class);
    }

    @Test
    void flag_isIdempotentPerReporter() {
        MessageReport first = threadService.flag(thread.getId(), bob.getId());
        MessageReport second = threadService.flag(thread.getId(), bob.getId());

        assertThat(first.getId()).isEqualTo(second.getId());
        assertThat(reportRepository.findPendingPage(dev.vepo.contraponto.shared.pagination.PageQuery.forGrid(20, 1)).total())
                                                                                                                             .isEqualTo(1);
    }

    @Test
    void reply_addsMessageOnOpenThread() {
        threadService.reply(thread.getId(), bob.getId(), "Thanks Alice");

        var view = threadService.loadThreadView(thread.getId(), alice.getId());
        assertThat(view.messages()).hasSize(2);
        assertThat(view.messages().get(1).getBody()).isEqualTo("Thanks Alice");
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        alice = Given.user()
                     .withUsername("thread-alice")
                     .withEmail("thread-alice@test.com")
                     .withName("Thread Alice")
                     .withPassword(PASSWORD)
                     .persist();
        bob = Given.user()
                   .withUsername("thread-bob")
                   .withEmail("thread-bob@test.com")
                   .withName("Thread Bob")
                   .withPassword(PASSWORD)
                   .persist();
        thread = composeService.compose(alice.getId(), bob.getUsername(), "Project chat", "Initial");
    }
}
