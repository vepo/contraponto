package dev.vepo.contraponto.messaging;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.shared.TestHttp;
import dev.vepo.contraponto.user.User;

@QuarkusIntegrationTest
class MessageRecipientSuggestionComponentTest {

    private static final String PASSWORD = "password123";

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        Given.cleanup();
        alice = Given.user()
                     .withUsername("suggest-alice")
                     .withEmail("suggest-alice@test.com")
                     .withName("Suggest Alice")
                     .withPassword(PASSWORD)
                     .persist();
        bob = Given.user()
                   .withUsername("compose-bob")
                   .withEmail("compose-bob@test.com")
                   .withName("Compose Bob")
                   .withPassword(PASSWORD)
                   .persist();
    }

    @Test
    void suggestionsOmitBlockedUsers() {
        Given.transaction(() -> Given.inject(UserBlockService.class).block(alice.getId(), bob.getId()));

        TestHttp.authenticated(alice)
                .header("HX-Request", "true")
                .get("/components/messages/recipient-suggestions?to=compose-b")
                .then()
                .statusCode(200)
                .body(not(containsString("compose-bob")));
    }

    @Test
    void suggestionsRequireAuthentication() {
        given().redirects()
               .follow(false)
               .get("/components/messages/recipient-suggestions?to=compose")
               .then()
               .statusCode(303);
    }

    @Test
    void suggestionsReturnMatchingUsernames() {
        TestHttp.authenticated(alice)
                .header("HX-Request", "true")
                .get("/components/messages/recipient-suggestions?to=compose-b")
                .then()
                .statusCode(200)
                .body(containsString("value=\"compose-bob\""))
                .body(containsString("Compose Bob"));
    }
}
