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
        viewRepository.recordView(post, null, "platform-guest-1", day, null);
        viewRepository.recordView(post, reader.getId(), "platform-user-1", day.plusHours(1), null);
        viewRepository.recordView(post, null, "platform-guest-2", day.plusHours(2), null);

        var range = monthRange(TestTimes.REFERENCE_MONTH);
        var counts = viewRepository.countDailyPlatform(range.start(), range.end());

        assertThat(counts.get(day.toLocalDate())).isEqualTo(3L);
    }

    @Test
    void countDailyUniqueVisitorsPlatformSplitsRegisteredAndGuest() {
        var post = publishedPost();
        var day = TestTimes.REFERENCE;
        viewRepository.recordView(post, null, "unique-guest-a", day, null);
        viewRepository.recordView(post, null, "unique-guest-a", day.plusMinutes(1), null);
        viewRepository.recordView(post, null, "unique-guest-b", day.plusMinutes(2), null);
        viewRepository.recordView(post, reader.getId(), "unique-user-a", day.plusMinutes(3), null);

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

        viewRepository.recordView(post, null, sessionId, viewedAt, null);
        viewRepository.recordView(post, reader.getId(), "other-session", viewedAt, null);

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

    @Test
    void topReferrersByBlogIdRanksByCountDescAndExcludesNulls() {
        var post = publishedPost();
        var day = TestTimes.REFERENCE;
        viewRepository.recordView(post, null, "referrer-a", day, "google.com");
        viewRepository.recordView(post, null, "referrer-b", day.plusMinutes(1), "google.com");
        viewRepository.recordView(post, null, "referrer-c", day.plusMinutes(2), "t.co");
        viewRepository.recordView(post, null, "referrer-d", day.plusMinutes(3), null);

        var range = monthRange(TestTimes.REFERENCE_MONTH);
        var top = viewRepository.topReferrersByBlogId(post.getBlog().getId(), range.start(), range.end(), 10);

        assertThat(top).extracting(ReferrerCount::host).containsExactly("google.com", "t.co");
        assertThat(top.get(0).views()).isEqualTo(2L);
        assertThat(top.get(1).views()).isEqualTo(1L);
    }

    @Test
    void topReferrersByBlogIdRespectsLimit() {
        var post = publishedPost();
        var day = TestTimes.REFERENCE;
        viewRepository.recordView(post, null, "limit-a", day, "one.example");
        viewRepository.recordView(post, null, "limit-b", day.plusMinutes(1), "two.example");
        viewRepository.recordView(post, null, "limit-c", day.plusMinutes(2), "three.example");

        var range = monthRange(TestTimes.REFERENCE_MONTH);
        var top = viewRepository.topReferrersByBlogId(post.getBlog().getId(), range.start(), range.end(), 2);

        assertThat(top).hasSize(2);
    }

    @Test
    void topReferrersPlatformAggregatesAcrossBlogs() {
        var post = publishedPost();
        var day = TestTimes.REFERENCE;
        viewRepository.recordView(post, null, "platform-referrer-a", day, "mastodon.social");
        viewRepository.recordView(post, null, "platform-referrer-b", day.plusMinutes(1), "mastodon.social");
        viewRepository.recordView(post, null, "platform-referrer-c", day.plusMinutes(2), null);

        var range = monthRange(TestTimes.REFERENCE_MONTH);
        var top = viewRepository.topReferrersPlatform(range.start(), range.end(), 10);

        assertThat(top).extracting(ReferrerCount::host).containsExactly("mastodon.social");
        assertThat(top.get(0).views()).isEqualTo(2L);
    }
}
