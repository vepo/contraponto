package dev.vepo.contraponto.activitypub;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebAuthTest;
import dev.vepo.contraponto.user.User;

@WebAuthTest
class ActivityPubWebTest {

    private static final String TEST_USER_PASSWORD = "fediversePass123";

    private User testUser;

    @Test
    void authorCanEnableFediversePublishing(App app) {
        app.login(testUser)
           .writingAppearance()
           .assertFediverseSectionVisible()
           .assertFediverseOptInChecked(false)
           .toggleFediverseOptIn(true)
           .submitFediverseSettings()
           .assertFediverseSuccessMessage("Fediverse publishing enabled")
           .refresh()
           .assertFediverseOptInChecked(true)
           .assertFediverseHandleVisible();
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        testUser = Given.user()
                        .withUsername("fediverseuser")
                        .withEmail("fediverse@example.com")
                        .withPassword(TEST_USER_PASSWORD)
                        .withName("Fediverse User")
                        .persist();
    }
}
