package dev.vepo.contraponto.activitypub.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import dev.vepo.contraponto.user.User;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class ActivityPubAuthorArchiveQueryTest {

    @Inject
    PostRepository postRepository;

    private User author;

    @Test
    void publishedArchiveInterleavesMainAndSecondaryByPublishedAt() {
        var secondary = Given.blog()
                             .withUser(author)
                             .withSlug("lab-notes")
                             .withName("Lab Notes")
                             .withDescription("Secondary archive")
                             .persist();

        Given.post()
             .withAuthor(author)
             .withTitle("Older Main")
             .withSlug("older-main")
             .withContent("Body")
             .withPublishedAt(LocalDateTime.of(2024, 1, 1, 10, 0))
             .persist();
        Given.post()
             .withAuthor(author)
             .withBlog(secondary)
             .withTitle("Middle Secondary")
             .withSlug("middle-secondary")
             .withContent("Body")
             .withPublishedAt(LocalDateTime.of(2024, 2, 1, 10, 0))
             .persist();
        Given.post()
             .withAuthor(author)
             .withTitle("Newer Main")
             .withSlug("newer-main")
             .withContent("Body")
             .withPublishedAt(LocalDateTime.of(2024, 3, 1, 10, 0))
             .persist();
        Given.post()
             .withAuthor(author)
             .withBlog(secondary)
             .withTitle("Draft Secondary")
             .withSlug("draft-secondary")
             .withContent("Body")
             .withPublished(false)
             .persist();

        assertThat(postRepository.countPublishedByAuthor(author.getId())).isEqualTo(3);

        var oldestFirst = postRepository.findPublishedByAuthorOldestFirst(author.getId());
        assertThat(oldestFirst).extracting(post -> post.getSlug())
                               .containsExactly("older-main", "middle-secondary", "newer-main");

        var newestFirst = postRepository.findPublishedByAuthor(author.getId(), PageQuery.forGrid(20, 1));
        assertThat(newestFirst.data()).extracting(post -> post.getSlug())
                                      .containsExactly("newer-main", "middle-secondary", "older-main");
        assertThat(newestFirst.total()).isEqualTo(3);
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        author = Given.user()
                      .withUsername("archiveuser")
                      .withEmail("archiveuser@example.com")
                      .withPassword("pw123456789")
                      .withName("Archive User")
                      .persist();
    }
}
