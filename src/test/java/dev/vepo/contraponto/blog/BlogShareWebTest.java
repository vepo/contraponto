package dev.vepo.contraponto.blog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebReaderTest;
import dev.vepo.contraponto.user.User;
import jakarta.persistence.EntityManager;

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

    @Test
    void mainBlogHomeWithBannerIncludesOgImage(App app) {
        Given.transaction(() -> {
            var mainBlog = author.getDefaultBlog();
            mainBlog.setBanner(Given.randomCover(mainBlog));
            Given.inject(EntityManager.class).merge(mainBlog);
        });

        app.access();
        app.goTo(author.getDefaultBlog());
        app.assertPageSourceContains("property=\"og:image\"")
           .assertPageSourceContains("name=\"twitter:card\" content=\"summary_large_image\"");
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
