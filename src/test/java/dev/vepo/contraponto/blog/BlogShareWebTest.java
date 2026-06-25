package dev.vepo.contraponto.blog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebReaderTest;
import dev.vepo.contraponto.user.User;

@WebReaderTest
class BlogShareWebTest {

    private User author;

    @Test
    void mainBlogHomeHasShareActions(App app) {
        app.access()
           .goTo(author.getDefaultBlog())
           .assertShareLinkedInHrefContains("shareblogauthor")
           .clickShareCopy()
           .assertShareCopyShowsCopied();
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        author = Given.user()
                      .withUsername("shareblogauthor")
                      .withEmail("shareblogauthor@test.com")
                      .withName("Share Blog Author")
                      .withPassword("password123")
                      .persist();
    }
}
