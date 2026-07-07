package dev.vepo.contraponto.messaging;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.shared.TestHttp;
import dev.vepo.contraponto.user.User;

@QuarkusIntegrationTest
class MessageMailboxEndpointTest {

    private User alice;
    private User bob;

    @Test
    void mailboxHubAndTabEndpointsAreReachable() {
        TestHttp.authenticated(alice)
                .get("/account/mailbox")
                .then()
                .statusCode(200)
                .body(containsString("messagesListContent"))
                .body(containsString("/account/messages/components/tab/open"));

        TestHttp.authenticated(alice)
                .get("/account/messages/components/tab/open")
                .then()
                .statusCode(200)
                .body(anyOf(containsString("message-thread-list"), containsString("messaging.mailbox.empty")));
    }

    @Test
    void openTabListsThreadsForParticipant() {
        Given.transaction(() -> {
            var composeService = Given.inject(MessageComposeService.class);
            composeService.compose(alice.getId(), bob.getUsername(), "Mailbox tab test", "Hello");
        });

        TestHttp.authenticated(alice)
                .get("/account/messages/components/tab/open")
                .then()
                .statusCode(200)
                .body(containsString("Mailbox tab test"))
                .body(not(containsString("loading-spinner")));
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        alice = Given.user()
                     .withUsername("mbox-alice")
                     .withEmail("mbox-alice@test.com")
                     .withName("Mailbox Alice")
                     .withPassword("password123")
                     .persist();
        bob = Given.user()
                   .withUsername("mbox-bob")
                   .withEmail("mbox-bob@test.com")
                   .withName("Mailbox Bob")
                   .withPassword("password123")
                   .persist();
    }

    @Test
    void tabRequiresAuthentication() {
        given().redirects()
               .follow(false)
               .get("/account/messages/components/tab/open")
               .then()
               .statusCode(303);
    }

    @Test
    void threadViewLoadsForParticipant() {
        long threadId = Given.transaction(() -> {
            var composeService = Given.inject(MessageComposeService.class);
            return composeService.compose(alice.getId(), bob.getUsername(), "Thread view test", "Body").getId();
        });

        TestHttp.authenticated(alice)
                .get("/account/messages/" + threadId)
                .then()
                .statusCode(200)
                .body(containsString("Thread view test"));
    }
}
