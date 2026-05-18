package dev.vepo.contraponto.auth;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.Given;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class PasswordRecoveryTest {

    private static final Pattern TOKEN_IN_LINK = Pattern.compile("/password-recovery/reset\\?token=([^\"'&\\s]+)");

    @Inject
    MockMailbox mailbox;

    @BeforeEach
    void clearMailbox() {
        mailbox.clear();
    }

    @Test
    void recoverySendsEmailAndAllowsReset() {
        var user = Given.user()
                        .withUsername("resetflow")
                        .withEmail("resetflow@example.com")
                        .withName("Reset Flow")
                        .withPassword("oldPass1234")
                        .persist();

        given().contentType("application/x-www-form-urlencoded")
               .formParam("email", user.getEmail())
               .when()
               .post("/forms/auth/password-recovery/request")
               .then()
               .statusCode(200);

        var messages = mailbox.getMessagesSentTo(user.getEmail());
        assertThat(messages).hasSize(1);
        String html = messages.get(0).getHtml();
        var matcher = TOKEN_IN_LINK.matcher(html);
        assertThat(matcher.find()).isTrue();
        String token = matcher.group(1);

        given().contentType("application/x-www-form-urlencoded")
               .formParam("token", token)
               .formParam("newPassword", "newPass5678")
               .formParam("confirmPassword", "newPass5678")
               .when()
               .post("/forms/auth/password-recovery/reset")
               .then()
               .statusCode(200);

        assertThat(mailbox.getMessagesSentTo(user.getEmail())).hasSize(2);

        given().contentType("application/x-www-form-urlencoded")
               .formParam("login", user.getEmail())
               .formParam("password", "newPass5678")
               .when()
               .post("/forms/auth/login")
               .then()
               .statusCode(200);
    }

    @Test
    void unknownEmailStillReturnsSuccessWithoutSending() {
        mailbox.clear();

        given().contentType("application/x-www-form-urlencoded")
               .formParam("email", "nobody@example.com")
               .when()
               .post("/forms/auth/password-recovery/request")
               .then()
               .statusCode(200);

        assertThat(mailbox.getTotalMessagesSent()).isZero();
    }

}
