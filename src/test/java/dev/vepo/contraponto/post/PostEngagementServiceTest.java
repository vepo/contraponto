package dev.vepo.contraponto.post;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class PostEngagementServiceTest {

    @Inject
    PostEngagementService postEngagementService;

    private User author;
    private User reader;

    private Post publishedPost() {
        return Given.post()
                    .withAuthor(author)
                    .withTitle("Engagement")
                    .withContent("Body")
                    .withSlug("engagement-post")
                    .withPublished(true)
                    .persist();
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        author = Given.user()
                      .withUsername("engageauthor")
                      .withEmail("engageauthor@test.com")
                      .withName("Engage Author")
                      .withPassword("Password123!")
                      .persist();
        reader = Given.user()
                      .withUsername("engagereader")
                      .withEmail("engagereader@test.com")
                      .withName("Engage Reader")
                      .withPassword("Password123!")
                      .persist();
    }

    @Test
    void shouldNotRecordReaderEngagementForAuthor() {
        var post = publishedPost();

        assertThat(postEngagementService.shouldRecordReaderEngagement(post, author.getId())).isFalse();
    }

    @Test
    void shouldRecordReaderEngagementForAnonymousViewer() {
        var post = publishedPost();

        assertThat(postEngagementService.shouldRecordReaderEngagement(post, null)).isTrue();
    }

    @Test
    void shouldRecordReaderEngagementForOtherUser() {
        var post = publishedPost();

        assertThat(postEngagementService.shouldRecordReaderEngagement(post, reader.getId())).isTrue();
    }
}
