package dev.vepo.contraponto.tag;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebTest;
import dev.vepo.contraponto.user.User;

@WebTest
class TagPageTest {

    private User author;

    @BeforeEach
    void setup() {
        Given.cleanup();
        author = Given.user()
                      .withUsername("tagauthor")
                      .withEmail("tagauthor@example.com")
                      .withPassword("tagauthorpass")
                      .withName("Tag Author")
                      .persist();
        String baseContent = """
                             Content of the blog post. Lorem ipsum dolor sit amet.
                             """;
        Given.post()
             .withTitle("Tagged Story")
             .withSlug("tagged-story")
             .withDescription("Has tags")
             .withContent(baseContent)
             .withAuthor(author)
             .withTags("news", "Java")
             .persist();
    }

    @Test
    void tagPageListsPostsWithThatTag(App app) {
        app.access()
           .goToTag("news")
           .assertListsPostTitle("Tagged Story");
    }
}
