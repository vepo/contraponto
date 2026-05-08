package dev.vepo.contraponto.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebTest;
import dev.vepo.contraponto.user.Role;
import dev.vepo.contraponto.user.User;

@WebTest
class ReviewTest {

    private User admin;
    private User regularUser;
    private Post post1;
    private Post post2;

    @Test
    void checkPromoteToFeatureButtonTest(App app) {
        app.access()
           .login(admin)
           .assertNumberOfPosts(0)
           .goToReview()
           .assertNumberOfPosts(2)
           .toggleFeatured(post1)
           .home()
           .assertNumberOfPosts(1);
    }

    @BeforeEach
    void setup() {
        Given.cleanup();
        admin = Given.user()
                     .withUsername("admin")
                     .withName("Admin")
                     .withEmail("admin@contraponto.com.br")
                     .withPassword("qwas1234")
                     .withRole(Role.ADMIN)
                     .persist();
        regularUser = Given.user()
                           .withUsername("regular-user")
                           .withName("Regular User")
                           .withEmail("regular-user@contraponto.com.br")
                           .withPassword("qwas1234")
                           .withRole(Role.ADMIN)
                           .persist();
        post1 = Given.post()
                     .withAuthor(regularUser)
                     .withTitle("Post 1")
                     .withSlug("post-1")
                     .withContent("Post 1 content")
                     .withCover(Given.randomCover())
                     .withDescription("Post 1 description")
                     .withPublished(true)
                     .withFeatured(false)
                     .persist();
        post2 = Given.post()
                     .withAuthor(regularUser)
                     .withTitle("Post 2")
                     .withSlug("post-2")
                     .withContent("Post 2 content")
                     .withCover(Given.randomCover())
                     .withDescription("Post 2 description")
                     .withPublished(true)
                     .withFeatured(false)
                     .persist();
    }
}
