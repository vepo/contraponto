package dev.vepo.contraponto.reddit.account;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebAuthTest;
import dev.vepo.contraponto.user.User;
import jakarta.persistence.EntityManager;

@WebAuthTest
class AuthorAppearanceRedditWebTest {

    private static final String TEST_USER_PASSWORD = "redditWebPass123";

    private User testUser;

    @Test
    void authorSeesConnectedStateAndCanDisconnect(App app) {
        Given.transaction(() -> {
            EntityManager em = Given.inject(EntityManager.class);
            User managed = em.find(User.class, testUser.getId());
            em.persist(new UserRedditAccount(managed, "coolredditor", "ciphertext"));
        });

        app.login(testUser)
           .writingAppearance()
           .assertRedditSectionVisible()
           .assertRedditConnectedAs("coolredditor")
           .clickRedditDisconnect()
           .assertRedditNotConnected();
    }

    @Test
    void authorSeesNotConnectedStateByDefault(App app) {
        app.login(testUser)
           .writingAppearance()
           .assertRedditSectionVisible()
           .assertRedditNotConnected();
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        testUser = Given.user()
                        .withUsername("reddituser")
                        .withEmail("reddituser@example.com")
                        .withPassword(TEST_USER_PASSWORD)
                        .withName("Reddit User")
                        .persist();
    }
}
