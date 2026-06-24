package dev.vepo.contraponto.readingtime;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.user.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ReadingTimeRepository {

    private static final Logger logger = LoggerFactory.getLogger(ReadingTimeRepository.class);
    private static final int MAX_SECONDS_PER_SESSION = 2 * 60 * 60;

    private final EntityManager entityManager;

    @Inject
    public ReadingTimeRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public void addSeconds(Post post, Long userId, String sessionId, int seconds, LocalDateTime activityAt) {
        // Native: PostgreSQL LEAST() for capped session increment is not expressible in
        // JPQL bulk update.
        int updated = entityManager.createNativeQuery("""
                                                      UPDATE tb_reading_sessions
                                                      SET total_seconds = LEAST(total_seconds + :seconds, :maxSeconds),
                                                          last_activity_at = :activityAt,
                                                          user_id = COALESCE(user_id, :userId)
                                                      WHERE post_id = :postId AND session_id = :sessionId
                                                      """)
                                   .setParameter("seconds", seconds)
                                   .setParameter("maxSeconds", MAX_SECONDS_PER_SESSION)
                                   .setParameter("activityAt", activityAt)
                                   .setParameter("userId", userId)
                                   .setParameter("postId", post.getId())
                                   .setParameter("sessionId", sessionId)
                                   .executeUpdate();

        if (updated > 0) {
            return;
        }

        var readingSession = new ReadingSession(post,
                                                userId != null ? entityManager.getReference(User.class, userId) : null,
                                                sessionId,
                                                activityAt);
        readingSession.setTotalSeconds(Math.min(seconds, MAX_SECONDS_PER_SESSION));
        readingSession.setLastActivityAt(activityAt);
        entityManager.persist(readingSession);
    }

    public long averageSecondsByPost(Post post) {
        Double average = entityManager.createQuery("""
                                                   SELECT AVG(rs.totalSeconds)
                                                   FROM ReadingSession rs
                                                   WHERE rs.post = :post AND rs.totalSeconds > 0
                                                   """, Double.class)
                                      .setParameter("post", post)
                                      .getSingleResult();
        if (average == null) {
            return 0L;
        }
        return Math.round(average);
    }

    public Map<LocalDate, Long> countDailySecondsByBlogId(long blogId,
                                                          LocalDateTime startInclusive,
                                                          LocalDateTime endExclusive) {
        // Native: CAST(… AS date) daily aggregation is database-specific.
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                                                              SELECT CAST(rs.last_activity_at AS date), COALESCE(SUM(rs.total_seconds), 0)
                                                              FROM tb_reading_sessions rs
                                                              JOIN tb_posts p ON rs.post_id = p.id
                                                              WHERE p.blog_id = :blogId
                                                                AND rs.last_activity_at >= :start
                                                                AND rs.last_activity_at < :end
                                                              GROUP BY CAST(rs.last_activity_at AS date)
                                                              ORDER BY 1
                                                              """)
                                           .setParameter("blogId", blogId)
                                           .setParameter("start", startInclusive)
                                           .setParameter("end", endExclusive)
                                           .getResultList();

        Map<LocalDate, Long> counts = new LinkedHashMap<>();
        for (Object[] row : rows) {
            LocalDate day = row[0] instanceof java.sql.Date sqlDate
                                                                    ? sqlDate.toLocalDate()
                                                                    : LocalDate.parse(row[0].toString());
            counts.put(day, ((Number) row[1]).longValue());
        }
        return counts;
    }

    @Transactional
    public void migrateAnonymousSessionsToUser(Long userId, String sessionId) {
        int updated = entityManager.createQuery("""
                                                UPDATE ReadingSession rs
                                                SET rs.user = :user
                                                WHERE rs.user IS NULL
                                                  AND rs.sessionId = :sessionId
                                                  AND rs.post.blog.owner.id <> :userId
                                                """)
                                   .setParameter("user", entityManager.getReference(User.class, userId))
                                   .setParameter("sessionId", sessionId)
                                   .setParameter("userId", userId)
                                   .executeUpdate();

        if (updated > 0) {
            logger.info("Migrated {} anonymous reading sessions to user ID {}", updated, userId);
        }
    }
}
