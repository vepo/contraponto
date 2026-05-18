package dev.vepo.contraponto.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.TestHttp;
import dev.vepo.contraponto.user.Role;
import dev.vepo.contraponto.user.User;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class AccountEmailsTest {

    @Inject
    MockMailbox mailbox;

    private User adminUser;

    @Test
    void adminPasswordResetSendsSecurityEmail() {
        int token = java.util.concurrent.ThreadLocalRandom.current().nextInt(100_000);
        String email = "n%d@example.com".formatted(token);
        var target = Given.user()
                          .withUsername("n%05d".formatted(token))
                          .withEmail(email)
                          .withName("Notify Me")
                          .withPassword("memberPass123")
                          .persist();

        mailbox.clear();

        TestHttp.authenticated(adminUser)
                .contentType("application/x-www-form-urlencoded")
                .formParam("id", target.getId())
                .formParam("username", target.getUsername())
                .formParam("name", target.getName())
                .formParam("email", target.getEmail())
                .formParam("active", "on")
                .formParam("roles", Role.USER.name())
                .formParam("newPassword", "newPass45678")
                .when()
                .post("/forms/users")
                .then()
                .statusCode(200);

        var messages = mailbox.getMessagesSentTo(email);
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getSubject()).contains("password was changed");
    }

    @BeforeEach
    void setup() {
        mailbox.clear();
        Given.cleanup();
        adminUser = Given.user()
                         .withUsername("useradmin")
                         .withEmail("useradmin@example.com")
                         .withName("User Admin")
                         .withPassword("userAdminPass123")
                         .withRoles(Role.USER, Role.USER_ADMINISTRATOR)
                         .persist();
    }
}
