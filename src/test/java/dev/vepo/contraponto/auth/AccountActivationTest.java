package dev.vepo.contraponto.auth;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.user.Role;
import io.quarkus.mailer.MockMailbox;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class AccountActivationTest {

    private static final Pattern TOKEN_IN_LINK = Pattern.compile("/account/activate\\?token=([^\"'&\\s]+)");

    private static final Pattern REPORT_LINK = Pattern.compile("/account/report-signup\\?token=([^\"'&\\s]+)");

    @Inject
    MockMailbox mailbox;

    @BeforeEach
    void clearMailbox() {
        mailbox.clear();
        Given.cleanup();
    }

    @Test
    void inactiveUserCannotLoginBeforeActivation() {
        given().contentType("application/x-www-form-urlencoded")
               .formParam("username", "pendinguser")
               .formParam("name", "Pending User")
               .formParam("email", "pending@example.com")
               .formParam("password", "Password123!")
               .when()
               .post("/forms/auth/signup")
               .then()
               .statusCode(200);

        given().contentType("application/x-www-form-urlencoded")
               .formParam("login", "pending@example.com")
               .formParam("password", "Password123!")
               .when()
               .post("/forms/auth/login")
               .then()
               .statusCode(400);
    }

    @Test
    void invalidActivationTokenShowsErrorPage() {
        given().when()
               .get("/account/activate?token=not-a-valid-token")
               .then()
               .statusCode(200)
               .body(org.hamcrest.Matchers.containsString("invalid or has expired"));
    }

    @Test
    void signupActivationEmailUsesRequestLocale() {
        given().contentType("application/x-www-form-urlencoded")
               .cookie("contraponto_locale", "en")
               .formParam("username", "localeuser")
               .formParam("name", "Locale User")
               .formParam("email", "localeuser@example.com")
               .formParam("password", "Password123!")
               .when()
               .post("/forms/auth/signup")
               .then()
               .statusCode(200);

        var messages = mailbox.getMessagesSentTo("localeuser@example.com");
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getSubject()).contains("Activate your");
        assertThat(messages.get(0).getHtml()).contains("Thanks for signing up");
    }

    @Test
    void signupSendsActivationEmailAndLinkActivatesAccount() {
        given().contentType("application/x-www-form-urlencoded")
               .formParam("username", "newactiv")
               .formParam("name", "New Activ")
               .formParam("email", "newactiv@example.com")
               .formParam("password", "Password123!")
               .when()
               .post("/forms/auth/signup")
               .then()
               .statusCode(200);

        var messages = mailbox.getMessagesSentTo("newactiv@example.com");
        assertThat(messages).hasSize(1);
        String html = messages.get(0).getHtml();
        var matcher = TOKEN_IN_LINK.matcher(html);
        assertThat(matcher.find()).isTrue();
        String token = matcher.group(1);

        var activationResponse = given().redirects().follow(false)
                                        .when()
                                        .get("/account/activate?token=%s".formatted(token))
                                        .then()
                                        .statusCode(303)
                                        .extract()
                                        .response();

        assertThat(activationResponse.getHeaders().getValues("Set-Cookie"))
                                                                           .anyMatch(cookie -> cookie.contains("__session="));

        given().contentType("application/x-www-form-urlencoded")
               .formParam("login", "newactiv@example.com")
               .formParam("password", "Password123!")
               .when()
               .post("/forms/auth/login")
               .then()
               .statusCode(200);
    }

    @Test
    void unauthorizedSignupReportNotifiesAdminAndInvalidatesActivation() {
        Given.user()
             .withUsername("siteadmin")
             .withEmail("admin-notify@test.com")
             .withPassword("AdminPass123!")
             .withName("Site Admin")
             .withRoles(Role.ADMIN, Role.USER)
             .persist();

        given().contentType("application/x-www-form-urlencoded")
               .formParam("username", "reported")
               .formParam("name", "Reported User")
               .formParam("email", "reported@example.com")
               .formParam("password", "Password123!")
               .when()
               .post("/forms/auth/signup")
               .then()
               .statusCode(200);

        var signupMessages = mailbox.getMessagesSentTo("reported@example.com");
        assertThat(signupMessages).hasSize(1);
        String html = signupMessages.get(0).getHtml();
        assertThat(html).contains("/account/report-signup?token=");

        var reportMatcher = REPORT_LINK.matcher(html);
        assertThat(reportMatcher.find()).isTrue();
        String reportToken = reportMatcher.group(1);

        given().when()
               .get("/account/report-signup?token=%s".formatted(reportToken))
               .then()
               .statusCode(200)
               .body(org.hamcrest.Matchers.anyOf(
                                                 org.hamcrest.Matchers.containsString("administrator has been notified"),
                                                 org.hamcrest.Matchers.containsString("administrador foi notificado"),
                                                 org.hamcrest.Matchers.containsString("administrador del sitio")));

        var adminMessages = mailbox.getMessagesSentTo("admin-notify@test.com");
        assertThat(adminMessages).hasSize(1);
        assertThat(adminMessages.get(0).getSubject()).contains("Unauthorized signup reported");
        assertThat(adminMessages.get(0).getHtml()).contains("reported@example.com");

        var activateMatcher = TOKEN_IN_LINK.matcher(html);
        assertThat(activateMatcher.find()).isTrue();
        String activateToken = activateMatcher.group(1);

        given().when()
               .get("/account/activate?token=%s".formatted(activateToken))
               .then()
               .statusCode(200)
               .body(org.hamcrest.Matchers.containsString("invalid or has expired"));
    }
}
