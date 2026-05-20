package dev.vepo.contraponto.tag;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebTest;
import dev.vepo.contraponto.user.User;

@WebTest
class TagPageMainAuthorsWebTest {

    @BeforeEach
    void setup() {
        Given.cleanup();
    }

    @Test
    void tagPageShowsMainAuthorsOrdered(App app) {
        User alice = Given.user()
                          .withUsername("tagpage-alice")
                          .withEmail("tagpage-alice@example.com")
                          .withName("Tagpage Alice")
                          .withPassword("pass12345")
                          .persist();
        User bob = Given.user()
                        .withUsername("tagpage-bob")
                        .withEmail("tagpage-bob@example.com")
                        .withName("Tagpage Bob")
                        .withPassword("pass12345")
                        .persist();

        Given.post().withTitle("A1").withSlug("a1").withContent("c").withAuthor(alice).withTags("topic-x").persist();
        Given.post().withTitle("A2").withSlug("a2").withContent("c").withAuthor(alice).withTags("topic-x").persist();
        Given.post().withTitle("B1").withSlug("b1").withContent("c").withAuthor(bob).withTags("topic-x").persist();

        app.goToPath("/tags/topic-x")
           .assertPageSourceContains("browse-explore-aside")
           .assertPageSourceContains("/authors/tagpage-alice")
           .assertPageSourceContains("/authors/tagpage-bob");
    }
}
