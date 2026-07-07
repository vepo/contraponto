package dev.vepo.contraponto.directory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebAuthTest;
import dev.vepo.contraponto.user.User;

@WebAuthTest
class AuthorProfileMessageWebTest {

    private static final String PASSWORD = "password123";

    private User author;
    private User reader;

    @Test
    void messageButtonLinksToComposeWithPrefill(App app) {
        app.login(reader)
           .goToPath("/authors/" + author.getUsername())
           .assertPageSourceContains("/account/messages/compose?to=" + author.getUsername());

        app.messagesCompose(author.getUsername())
           .fillComposeTitle("Hello")
           .fillComposeBody("From profile CTA");
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        author = Given.user()
                      .withUsername("profmsg-auth")
                      .withEmail("profmsg-auth@test.com")
                      .withName("Profile Msg Author")
                      .withPassword(PASSWORD)
                      .persist();
        reader = Given.user()
                      .withUsername("profmsg-read")
                      .withEmail("profmsg-read@test.com")
                      .withName("Profile Msg Reader")
                      .withPassword(PASSWORD)
                      .persist();
        Given.post()
             .withAuthor(author)
             .withTitle("Author post")
             .withSlug("author-post-msg")
             .withContent("Body")
             .withPublished(true)
             .persist();
    }
}
