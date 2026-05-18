package dev.vepo.contraponto.post;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebTest;
import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.user.User;
import jakarta.inject.Inject;

@WebTest
class PostChangeHistoryTest {

    @Inject
    PostRepository postRepository;

    @Inject
    PostPublicationService publicationService;

    private User author;
    private Post post;

    @Test
    void changeHistory_showsVersionInMetadataAndDiffInModal(App app) {
        app.access()
           .goTo(post)
           .assertVersionInMetadata(2)
           .openChangeHistoryModal()
           .assertChangeHistoryModalTitle()
           .expandFirstChangeDetails()
           .assertChangeDiffVisible()
           .closeChangeHistoryModal();
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        author = Given.user()
                      .withUsername("historyauthor")
                      .withEmail("historyauthor@test.com")
                      .withName("History Author")
                      .withPassword("password123")
                      .persist();
        post = Given.post()
                    .withAuthor(author)
                    .withTitle("History Test Post")
                    .withContent("Version one content")
                    .withSlug("history-test-post")
                    .withPublished(true)
                    .persist();

        Given.transaction(() -> {
            var reloaded = postRepository.findById(post.getId()).orElseThrow();
            reloaded.setContent("Version two content");
            publicationService.publish(reloaded);
        });
        post = postRepository.findMainBlogPost(author.getUsername(), "history-test-post").orElseThrow();
    }
}
