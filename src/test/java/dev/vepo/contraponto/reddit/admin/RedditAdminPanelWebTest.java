package dev.vepo.contraponto.reddit.admin;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.reddit.RedditSettings;
import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebPlatformTest;
import dev.vepo.contraponto.user.Role;
import dev.vepo.contraponto.user.User;
import jakarta.inject.Inject;

@WebPlatformTest
class RedditAdminPanelWebTest {

    private static final String PASSWORD = "redditAdminWebPass123";

    @Inject
    RedditSettings settings;

    private User admin;

    @Test
    void adminCanViewAndToggleKillSwitch(App app) {
        app.login(admin)
           .goToPath("/administration/reddit")
           .assertPageSourceContains("Platform-wide Reddit syndication settings")
           .toggleRedditPlatformSyndication()
           .submitRedditPlatformSettings()
           .assertRedditPlatformMessage("globally disabled");

        assertThat(settings.platformSyndicationEnabled()).isFalse();

        app.toggleRedditPlatformSyndication()
           .submitRedditPlatformSettings()
           .assertRedditPlatformMessage("globally enabled");

        assertThat(settings.platformSyndicationEnabled()).isTrue();
    }

    @Test
    void nonAdminCannotAccessRedditPanel(App app) {
        var regular = Given.user()
                           .withUsername("redditpanelreg")
                           .withEmail("redditpanelreg@example.com")
                           .withName("Reddit Panel Regular")
                           .withPassword(PASSWORD)
                           .persist();

        app.login(regular)
           .goToPath("/administration/reddit")
           .assertRedditPlatformPanelNotLoaded();
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        settings.setPlatformSyndicationEnabled(true);
        admin = Given.user()
                     .withUsername("redditpaneladm")
                     .withEmail("redditpaneladmin@example.com")
                     .withName("Reddit Panel Admin")
                     .withPassword(PASSWORD)
                     .withRole(Role.ADMIN)
                     .persist();
    }
}
