package dev.vepo.contraponto.auth;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.TestHttp;
import dev.vepo.contraponto.user.User;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class EmailVerificationTest {

    private static final Pattern TOKEN_IN_LINK = Pattern.compile("/account/verify-email\\?token=([^\"'&\\s]+)");

    @Inject
    MockMailbox mailbox;

    private User user;

    @Test
    void profileEmailChangeRequiresVerification() {
        TestHttp.authenticated(user)
                .contentType("application/x-www-form-urlencoded")
                .formParam("name", user.getName())
                .formParam("email", "verify-new@example.com")
                .formParam("currentPassword", "verifyPass12")
                .when()
                .post("/forms/account/security")
                .then()
                .statusCode(200)
                .body(org.hamcrest.Matchers.containsString("Check your new email to confirm the address change."));

        var messages = mailbox.getMessagesSentTo("verify-new@example.com");
        assertThat(messages).hasSize(1);

        var matcher = TOKEN_IN_LINK.matcher(messages.get(0).getHtml());
        assertThat(matcher.find()).isTrue();
        String token = matcher.group(1);

        given().redirects()
               .follow(true)
               .when()
               .get("/account/verify-email?token=" + token)
               .then()
               .statusCode(200);

        assertThat(mailbox.getMessagesSentTo("verify-old@example.com")).hasSize(1);

        TestHttp.session(user)
                .when()
                .get("/account/security?verified=true")
                .then()
                .statusCode(200)
                .body(org.hamcrest.Matchers.containsString("Email address updated."));
    }

    @BeforeEach
    void setup() {
        mailbox.clear();
        Given.cleanup();
        user = Given.user()
                    .withUsername("emailverify")
                    .withEmail("verify-old@example.com")
                    .withName("Email Verify")
                    .withPassword("verifyPass12")
                    .persist();
    }
}
