package dev.vepo.contraponto.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.YearMonth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.shared.TestTimes;
import dev.vepo.contraponto.user.User;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@QuarkusIntegrationTest
class ViewRepositoryTest {

    private record MonthRange(LocalDateTime start, LocalDateTime end) {}

    @Inject
    ViewRepository viewRepository;

    @Inject
    EntityManager entityManager;
    private User author;

    private User reader;

    @Test
    void countDailyPlatformAggregatesViewsByDay() {
        var post = publishedPost();
        var day = TestTimes.REFERENCE;
        viewRepository.recordView(post, null, "platform-guest-1", day);
        viewRepository.recordView(post, reader.getId(), "platform-user-1", day.plusHours(1));
        viewRepository.recordView(post, null, "platform-guest-2", day.plusHours(2));

        var range = monthRange(TestTimes.REFERENCE_MONTH);
        var counts = viewRepository.countDailyPlatform(range.start(), range.end());

        assertThat(counts.get(day.toLocalDate())).isEqualTo(3L);
    }

    @Test
    void countDailyUniqueVisitorsPlatformSplitsRegisteredAndGuest() {
        var post = publishedPost();
        var day = TestTimes.REFERENCE;
        viewRepository.recordView(post, null, "unique-guest-a", day);
        viewRepository.recordView(post, null, "unique-guest-a", day.plusMinutes(1));
        viewRepository.recordView(post, null, "unique-guest-b", day.plusMinutes(2));
        viewRepository.recordView(post, reader.getId(), "unique-user-a", day.plusMinutes(3));

        var range = monthRange(TestTimes.REFERENCE_MONTH);
        var daily = viewRepository.countDailyUniqueVisitorsPlatform(range.start(), range.end());
        var monthly = viewRepository.countMonthlyUniqueVisitorsPlatform(range.start(), range.end());

        var split = daily.get(day.toLocalDate());
        assertThat(split.registeredVisitors()).isEqualTo(1L);
        assertThat(split.guestVisitors()).isEqualTo(2L);
        assertThat(monthly.registeredVisitors()).isEqualTo(1L);
        assertThat(monthly.guestVisitors()).isEqualTo(2L);
    }

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

    private MonthRange monthRange(YearMonth yearMonth) {
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.plusMonths(1).atDay(1).atStartOfDay();
        return new MonthRange(start, end);
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
