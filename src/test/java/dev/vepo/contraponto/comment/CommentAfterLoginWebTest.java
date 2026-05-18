package dev.vepo.contraponto.comment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.components.forms.SignUpEndpoint;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebTest;
import dev.vepo.contraponto.user.User;

@WebTest
class CommentAfterLoginWebTest {

    private User author;
    private User reader;
    private Post post;

    @Test
    void comments_form_appears_after_login_modal(App app) {
        app.access();
        var postPage = app.goTo(post);
        postPage.assertCommentsSignInGateVisible();

        app.loginModal()
           .useLogin("commentreader@test.com")
           .usePassword("password123")
           .submit()
           .assertModalWasClosed()
           .assertMenuIsDisplayed()
           .assertCookieIsPresent(SignUpEndpoint.SESSION_COOKIE_NAME);

        postPage.waitForReady()
                .assertCommentsFormVisible();
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        author = Given.user()
                      .withUsername("commentweb")
                      .withEmail("commentweb@test.com")
                      .withName("Comment Web Author")
                      .withPassword("password123")
                      .persist();
        reader = Given.user()
                      .withUsername("commentreader")
                      .withEmail("commentreader@test.com")
                      .withName("Comment Web Reader")
                      .withPassword("password123")
                      .persist();
        post = Given.post()
                    .withAuthor(author)
                    .withTitle("Comment web test")
                    .withSlug("comment-web-test")
                    .withContent("Post body")
                    .withDescription("Description")
                    .withPublished(true)
                    .persist();
    }
}
