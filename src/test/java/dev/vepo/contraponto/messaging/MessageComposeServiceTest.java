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
import jakarta.ws.rs.ForbiddenException;

@QuarkusIntegrationTest
class MessageComposeServiceTest {

    private static final String PASSWORD = "password123";

    @Inject
    MessageComposeService composeService;

    @Inject
    UserBlockRepository blockRepository;

    @Inject
    UserBlockService blockService;

    private User alice;
    private User bob;

    @Test
    void compose_createsOpenThreadWithFirstMessage() {
        MessageThread thread = composeService.compose(alice.getId(), bob.getUsername(), "Hello Bob", "First message body");

        assertThat(thread.getTitle()).isEqualTo("Hello Bob");
        assertThat(thread.getStatus()).isEqualTo(MessageThreadStatus.OPEN);
        assertThat(thread.getInitiator().getId()).isEqualTo(alice.getId());
        assertThat(thread.getRecipient().getId()).isEqualTo(bob.getId());
    }

    @Test
    void compose_enforcesRateLimit() {
        for (int i = 0; i < 10; i++) {
            User target = Given.user()
                               .withUsername("target" + i)
                               .withEmail("target" + i + "@test.com")
                               .withName("Target " + i)
                               .withPassword(PASSWORD)
                               .persist();
            composeService.compose(alice.getId(), target.getUsername(), "Title " + i, "Body " + i);
        }

        User extra = Given.user()
                          .withUsername("target-extra")
                          .withEmail("target-extra@test.com")
                          .withName("Target Extra")
                          .withPassword(PASSWORD)
                          .persist();

        assertThatThrownBy(() -> composeService.compose(alice.getId(), extra.getUsername(), "One more", "Body"))
                                                                                                                .isInstanceOf(BadRequestException.class)
                                                                                                                .hasMessageContaining("limit");
    }

    @Test
    void compose_rejectsWhenBlocked() {
        blockService.block(alice.getId(), bob.getId(), "spam");

        assertThatThrownBy(() -> composeService.compose(bob.getId(), alice.getUsername(), "Hi", "Body"))
                                                                                                        .isInstanceOf(ForbiddenException.class);
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        alice = Given.user()
                     .withUsername("compose-alice")
                     .withEmail("compose-alice@test.com")
                     .withName("Compose Alice")
                     .withPassword(PASSWORD)
                     .persist();
        bob = Given.user()
                   .withUsername("compose-bob")
                   .withEmail("compose-bob@test.com")
                   .withName("Compose Bob")
                   .withPassword(PASSWORD)
                   .persist();
    }
}
