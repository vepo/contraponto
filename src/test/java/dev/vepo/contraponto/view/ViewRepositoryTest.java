package dev.vepo.contraponto.view;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.TestTimes;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@QuarkusIntegrationTest
class ViewRepositoryTest {

    @Inject
    ViewRepository viewRepository;

    @Inject
    EntityManager entityManager;

    private User author;
    private User reader;

    @Test
    void migrateAnonymousViewsToUserSkipsOwnPosts() {
        var post = publishedPost();
        var viewedAt = TestTimes.REFERENCE;
        var sessionId = "migrate-skip-session";

        viewRepository.recordView(post, null, sessionId, viewedAt);
        viewRepository.recordView(post, reader.getId(), "other-session", viewedAt);

        viewRepository.migrateAnonymousViewsToUser(author.getId(), sessionId);

        assertThat(viewRepository.countByPost(post)).isEqualTo(2L);
        Number anonymousOwnPostViews = (Number) entityManager.createNativeQuery("""
                                                                                SELECT COUNT(*) FROM tb_views
                                                                                WHERE post_id = :postId
                                                                                  AND session_id = :sessionId
                                                                                  AND user_id IS NULL
                                                                                """)
                                                             .setParameter("postId", post.getId())
                                                             .setParameter("sessionId", sessionId)
                                                             .getSingleResult();
        assertThat(anonymousOwnPostViews.intValue()).isEqualTo(1);
    }

    private Post publishedPost() {
        return Given.post()
                    .withAuthor(author)
                    .withTitle("Views")
                    .withContent("Body")
                    .withSlug("views-post")
                    .withPublished(true)
                    .persist();
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        author = Given.user()
                      .withUsername("viewauthor")
                      .withEmail("viewauthor@test.com")
                      .withName("View Author")
                      .withPassword("Password123!")
                      .persist();
        reader = Given.user()
                      .withUsername("viewreader")
                      .withEmail("viewreader@test.com")
                      .withName("View Reader")
                      .withPassword("Password123!")
                      .persist();
    }
}
