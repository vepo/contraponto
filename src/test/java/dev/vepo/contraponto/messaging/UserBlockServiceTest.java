package dev.vepo.contraponto.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.user.User;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class UserBlockServiceTest {

    private static final String PASSWORD = "password123";

    @Inject
    MessageComposeService composeService;

    @Inject
    MessageThreadService threadService;

    @Inject
    UserBlockService blockService;

    @Inject
    MessageThreadRepository threadRepository;

    private User alice;
    private User bob;
    private MessageThread thread;

    @Test
    void block_freezesOpenThreadForBothParticipants() {
        blockService.block(alice.getId(), bob.getId(), "harassment");

        MessageThread reloaded = threadRepository.findById(thread.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(MessageThreadStatus.FROZEN);

        var aliceView = threadService.loadThreadView(thread.getId(), alice.getId());
        var bobView = threadService.loadThreadView(thread.getId(), bob.getId());
        assertThat(aliceView.showBlockedBanner()).isTrue();
        assertThat(bobView.showBlockedBanner()).isTrue();
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        alice = Given.user()
                     .withUsername("block-alice")
                     .withEmail("block-alice@test.com")
                     .withName("Block Alice")
                     .withPassword(PASSWORD)
                     .persist();
        bob = Given.user()
                   .withUsername("block-bob")
                   .withEmail("block-bob@test.com")
                   .withName("Block Bob")
                   .withPassword(PASSWORD)
                   .persist();
        thread = composeService.compose(alice.getId(), bob.getUsername(), "Chat", "Hello");
    }
}
