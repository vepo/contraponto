package dev.vepo.contraponto.post;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebReaderTest;
import dev.vepo.contraponto.user.User;

@WebReaderTest
class PostShareWebTest {

    private User author;
    private Post post;

    @Test
    void publishedPostShareActionsWork(App app) {
        app.access()
           .goTo(post)
           .assertShareLinkedInHrefContains("shareable-post-title")
           .clickShareCopy()
           .assertShareCopyShowsCopied();
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        author = Given.user()
                      .withUsername("sharepostauthor")
                      .withEmail("sharepostauthor@test.com")
                      .withName("Share Post Author")
                      .withPassword("password123")
                      .persist();
        post = Given.post()
                    .withAuthor(author)
                    .withTitle("Shareable Post Title")
                    .withSlug("shareable-post-title")
                    .withContent("Shareable post body for share actions test.")
                    .withPublished(true)
                    .persist();
    }
}
