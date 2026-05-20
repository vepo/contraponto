package dev.vepo.contraponto.tag;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class TagProfileServiceTest {

    @Inject
    TagProfileService tagProfileService;

    @Test
    void ordersMainAuthorsByPostCountOnTag() {
        User alice = Given.user()
                          .withUsername("tag-alice")
                          .withEmail("tag-alice@example.com")
                          .withName("Tag Alice")
                          .withPassword("pass12345")
                          .persist();
        User bob = Given.user()
                        .withUsername("tag-bob")
                        .withEmail("tag-bob@example.com")
                        .withName("Tag Bob")
                        .withPassword("pass12345")
                        .persist();

        Given.post().withTitle("Alice 1").withSlug("alice-1").withContent("c").withAuthor(alice).withTags("shared-topic").persist();
        Given.post().withTitle("Alice 2").withSlug("alice-2").withContent("c").withAuthor(alice).withTags("shared-topic").persist();
        Given.post().withTitle("Bob 1").withSlug("bob-1").withContent("c").withAuthor(bob).withTags("shared-topic").persist();

        var mainAuthors = tagProfileService.mainAuthorsForTag("shared-topic", 6);

        assertThat(mainAuthors).hasSize(2);
        assertThat(mainAuthors.get(0).author().getUsername()).isEqualTo("tag-alice");
        assertThat(mainAuthors.get(0).postCount()).isEqualTo(2);
        assertThat(mainAuthors.get(1).author().getUsername()).isEqualTo("tag-bob");
        assertThat(mainAuthors.get(1).postCount()).isEqualTo(1);
    }

    @BeforeEach
    void setup() {
        Given.cleanup();
    }
}
