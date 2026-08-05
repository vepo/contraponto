package dev.vepo.contraponto.blog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.reddit.account.UserRedditAccount;
import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebAuthorTest;
import dev.vepo.contraponto.user.User;
import jakarta.persistence.EntityManager;

@WebAuthorTest
class BlogManageRedditSectionWebTest {

    private static final String USER_EMAIL = "redditblogmanage@example.com";
    private static final String USER_NAME = "Reddit Blog Manage Tester";
    private static final String USER_PASSWORD = "redditBlogManagePass123";
    private static final String USER_USERNAME = "redditblogmgr";

    private User testUser;

    @Test
    void editFormEnablesToggleOnceAccountLinked(App app) {
        Given.transaction(() -> {
            EntityManager em = Given.inject(EntityManager.class);
            User managed = em.find(User.class, testUser.getId());
            em.persist(new UserRedditAccount(managed, "coolredditor", "ciphertext"));
        });

        app.login(testUser)
           .blogs()
           .clickEdit(USER_NAME)
           .assertTitle("Edit Blog")
           .assertRedditSectionEnabled();
    }

    @Test
    void editFormShowsDisabledToggleWhenNoAccountLinked(App app) {
        app.login(testUser)
           .blogs()
           .clickEdit(USER_NAME)
           .assertTitle("Edit Blog")
           .assertRedditSectionDisabled();
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        testUser = Given.user()
                        .withUsername(USER_USERNAME)
                        .withEmail(USER_EMAIL)
                        .withPassword(USER_PASSWORD)
                        .withName(USER_NAME)
                        .persist();
    }
}
