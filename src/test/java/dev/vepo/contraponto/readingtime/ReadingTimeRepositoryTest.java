package dev.vepo.contraponto.readingtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class ReadingTimeRepositoryTest {

    @Inject
    ReadingTimeRepository readingTimeRepository;

    private User author;

    @Test
    void averageSecondsByPostUsesSessionMean() {
        var post = Given.post()
                        .withAuthor(author)
                        .withTitle("Average")
                        .withContent("Body")
                        .withSlug("average-post")
                        .withPublished(true)
                        .persist();
        var now = LocalDateTime.now();

        readingTimeRepository.addSeconds(post, null, "session-a", 120, now);
        readingTimeRepository.addSeconds(post, null, "session-b", 180, now);

        assertThat(readingTimeRepository.averageSecondsByPost(post)).isEqualTo(150L);
    }

    @Test
    void countDailySecondsByBlogIdAggregatesByActivityDay() {
        var blog = author.getDefaultBlog();
        var post = Given.post()
                        .withAuthor(author)
                        .withBlog(blog)
                        .withTitle("Daily")
                        .withContent("Body")
                        .withSlug("daily-post")
                        .withPublished(true)
                        .persist();

        var month = YearMonth.now();
        var day = month.atDay(10);
        readingTimeRepository.addSeconds(post, null, "daily-session", 300, day.atTime(12, 0));

        var counts = readingTimeRepository.countDailySecondsByBlogId(blog.getId(),
                                                                     month.atDay(1).atStartOfDay(),
                                                                     month.plusMonths(1).atDay(1).atStartOfDay());
        assertThat(counts.get(LocalDate.from(day))).isEqualTo(300L);
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        author = Given.user()
                      .withUsername("rtrepo")
                      .withEmail("rtrepo@test.com")
                      .withName("RT Repo")
                      .withPassword("Password123!")
                      .persist();
    }
}
