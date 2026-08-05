package dev.vepo.contraponto.reddit.admin;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.reddit.RedditSettings;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.shared.TestHttp;
import dev.vepo.contraponto.user.Role;
import dev.vepo.contraponto.user.User;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class RedditPlatformSettingsEndpointTest {

    private static final String PASSWORD = "redditAdminPass123";

    @Inject
    RedditSettings settings;

    private User admin;
    private User nonAdmin;

    @Test
    void nonAdminCannotToggle() {
        TestHttp.authenticated(nonAdmin)
                .contentType("application/x-www-form-urlencoded")
                .formParam("platformSyndicationEnabled", "false")
                .post("/forms/administration/reddit")
                .then()
                .statusCode(403);

        assertThat(settings.platformSyndicationEnabled()).isTrue();
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        admin = Given.user()
                     .withUsername("redditadmin")
                     .withEmail("redditadmin@test.com")
                     .withName("Reddit Admin")
                     .withPassword(PASSWORD)
                     .withRole(Role.ADMIN)
                     .persist();
        nonAdmin = Given.user()
                        .withUsername("redditnonadmin")
                        .withEmail("redditnonadmin@test.com")
                        .withName("Reddit Non Admin")
                        .withPassword(PASSWORD)
                        .persist();
    }

    @Test
    void toggleOffThenOnAsAdmin() {
        TestHttp.authenticated(admin)
                .contentType("application/x-www-form-urlencoded")
                .post("/forms/administration/reddit")
                .then()
                .statusCode(200)
                .body(org.hamcrest.Matchers.containsString("globally disabled"));

        assertThat(settings.platformSyndicationEnabled()).isFalse();

        TestHttp.authenticated(admin)
                .contentType("application/x-www-form-urlencoded")
                .formParam("platformSyndicationEnabled", "true")
                .post("/forms/administration/reddit")
                .then()
                .statusCode(200)
                .body(org.hamcrest.Matchers.containsString("globally enabled"));

        assertThat(settings.platformSyndicationEnabled()).isTrue();
    }
}
