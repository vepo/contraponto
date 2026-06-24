package dev.vepo.contraponto.view;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
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
public class ViewRepository {

    private static final Logger logger = LoggerFactory.getLogger(ViewRepository.class);
    private final EntityManager entityManager;

    @Inject
    public ViewRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public long countByPost(Post post) {
        return entityManager.createQuery("SELECT COUNT(v) FROM View v WHERE v.post = :post", Long.class)
                            .setParameter("post", post)
                            .getSingleResult();
    }

    public Map<LocalDate, Long> countDailyByBlogId(long blogId, LocalDateTime startInclusive, LocalDateTime endExclusive) {
        // Native: CAST(viewed_at AS date) for daily GROUP BY is database-specific.
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                                                              SELECT CAST(v.viewed_at AS date), COUNT(*)
                                                              FROM tb_views v
                                                              JOIN tb_posts p ON v.post_id = p.id
                                                              WHERE p.blog_id = :blogId
                                                                AND v.viewed_at >= :start
                                                                AND v.viewed_at < :end
                                                              GROUP BY CAST(v.viewed_at AS date)
                                                              ORDER BY 1
                                                              """)
                                           .setParameter("blogId", blogId)
                                           .setParameter("start", startInclusive)
                                           .setParameter("end", endExclusive)
                                           .getResultList();

        return toDailyCounts(rows);
    }

    public Map<LocalDate, Long> countDailyPlatform(LocalDateTime startInclusive, LocalDateTime endExclusive) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                                                              SELECT CAST(viewed_at AS date), COUNT(*)
                                                              FROM tb_views
                                                              WHERE viewed_at >= :start
                                                                AND viewed_at < :end
                                                              GROUP BY CAST(viewed_at AS date)
                                                              ORDER BY 1
                                                              """)
                                           .setParameter("start", startInclusive)
                                           .setParameter("end", endExclusive)
                                           .getResultList();

        return toDailyCounts(rows);
    }

    public Map<LocalDate, DailyUniqueVisitors> countDailyUniqueVisitorsPlatform(LocalDateTime startInclusive,
                                                                                LocalDateTime endExclusive) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                                                              SELECT CAST(viewed_at AS date),
                                                                     COUNT(DISTINCT user_id) FILTER (WHERE user_id IS NOT NULL),
                                                                     COUNT(DISTINCT session_id) FILTER (WHERE user_id IS NULL)
                                                              FROM tb_views
                                                              WHERE viewed_at >= :start
                                                                AND viewed_at < :end
                                                              GROUP BY CAST(viewed_at AS date)
                                                              ORDER BY 1
                                                              """)
                                           .setParameter("start", startInclusive)
                                           .setParameter("end", endExclusive)
                                           .getResultList();

        Map<LocalDate, DailyUniqueVisitors> counts = new LinkedHashMap<>();
        for (Object[] row : rows) {
            LocalDate day = toLocalDate(row[0]);
            long registered = ((Number) row[1]).longValue();
            long guest = ((Number) row[2]).longValue();
            counts.put(day, new DailyUniqueVisitors(registered, guest));
        }
        return counts;
    }

    public DailyUniqueVisitors countMonthlyUniqueVisitorsPlatform(LocalDateTime startInclusive,
                                                                  LocalDateTime endExclusive) {
        Object[] row = (Object[]) entityManager.createNativeQuery("""
                                                                  SELECT COUNT(DISTINCT user_id) FILTER (WHERE user_id IS NOT NULL),
                                                                         COUNT(DISTINCT session_id) FILTER (WHERE user_id IS NULL)
                                                                  FROM tb_views
                                                                  WHERE viewed_at >= :start
                                                                    AND viewed_at < :end
                                                                  """)
                                               .setParameter("start", startInclusive)
                                               .setParameter("end", endExclusive)
                                               .getSingleResult();
        return new DailyUniqueVisitors(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
    }

    public Map<Long, Long> getViewCountsForPosts(List<Long> postIds) {
        if (postIds.isEmpty()) {
            return Collections.emptyMap();
        }

        var counts = entityManager.createQuery("""
                                               SELECT v.post.id, COUNT(v)
                                               FROM View v
                                               WHERE v.post.id IN :postIds
                                               GROUP BY v.post.id
                                               """, Object[].class)
                                  .setParameter("postIds", postIds)
                                  .getResultList();

        Map<Long, Long> viewCounts = new HashMap<>();
        for (Object[] row : counts) {
            Long postId = (Long) row[0];
            Long count = (Long) row[1];
            viewCounts.put(postId, count);
        }
        return viewCounts;
    }

    @Transactional
    public void migrateAnonymousViewsToUser(Long userId, String sessionId) {
        int updated = entityManager.createQuery("""
                                                UPDATE View v
                                                SET v.user = :user
                                                WHERE v.user IS NULL
                                                  AND v.sessionId = :sessionId
                                                  AND v.post.blog.owner.id <> :userId
                                                """)
                                   .setParameter("user", entityManager.getReference(User.class, userId))
                                   .setParameter("sessionId", sessionId)
                                   .setParameter("userId", userId)
                                   .executeUpdate();

        if (updated > 0) {
            logger.info("Migrated {} anonymous views to user ID {}", updated, userId);
        }
    }

    @Transactional
    public void recordView(Post post, Long userId, String sessionId, LocalDateTime viewedAt) {
        logger.info("Creating view for post! userId={} post={}", userId, post);
        // Native: direct INSERT for view deduplication relies on unique constraint
        // handling.
        String sql = """
                     INSERT INTO tb_views (post_id, user_id, session_id, viewed_at)
                     VALUES (:postId, :userId, :sessionId, :viewedAt)
                     """;
        entityManager.createNativeQuery(sql)
                     .setParameter("postId", post.getId())
                     .setParameter("userId", userId != null ? userId : null)
                     .setParameter("sessionId", sessionId)
                     .setParameter("viewedAt", viewedAt)
                     .executeUpdate();
    }

    private Map<LocalDate, Long> toDailyCounts(List<Object[]> rows) {
        Map<LocalDate, Long> counts = new LinkedHashMap<>();
        for (Object[] row : rows) {
            LocalDate day = toLocalDate(row[0]);
            counts.put(day, ((Number) row[1]).longValue());
        }
        return counts;
    }

    private LocalDate toLocalDate(Object value) {
        return value instanceof java.sql.Date sqlDate ? sqlDate.toLocalDate() : LocalDate.parse(value.toString());
    }
}
