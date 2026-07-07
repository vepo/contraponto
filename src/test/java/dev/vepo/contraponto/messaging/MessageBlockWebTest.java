package dev.vepo.contraponto.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebAuthTest;
import dev.vepo.contraponto.user.User;

@WebAuthTest
class MessageBlockWebTest {

    private static final String PASSWORD = "password123";

    private User alice;
    private User bob;
    private MessageThread thread;

    @Test
    void blockShowsBannerForBothParticipants(App app) {
        app.login(alice)
           .messagesThread(thread.getId())
           .assertBlockedBannerVisible();

        app.logout()
           .login(bob)
           .messagesThread(thread.getId())
           .assertBlockedBannerVisible();
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        alice = Given.user()
                     .withUsername("webblock-alice")
                     .withEmail("webblock-alice@test.com")
                     .withName("Web Block Alice")
                     .withPassword(PASSWORD)
                     .persist();
        bob = Given.user()
                   .withUsername("webblock-bob")
                   .withEmail("webblock-bob@test.com")
                   .withName("Web Block Bob")
                   .withPassword(PASSWORD)
                   .persist();
        Given.transaction(() -> {
            var composeService = Given.inject(MessageComposeService.class);
            var blockService = Given.inject(UserBlockService.class);
            thread = composeService.compose(alice.getId(), bob.getUsername(), "Block test", "Hi");
            blockService.block(alice.getId(), bob.getId(), "testing");
        });
    }
}
