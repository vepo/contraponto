package dev.vepo.contraponto.view;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
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
        // Update all anonymous views with matching session ID to this user
        int updated = entityManager.createQuery("""
                                                UPDATE View v
                                                SET v.user = :user
                                                WHERE v.user IS NULL AND v.sessionId = :sessionId
                                                """)
                                   .setParameter("user", entityManager.getReference(User.class, userId))
                                   .setParameter("sessionId", sessionId)
                                   .executeUpdate();

        if (updated > 0) {
            logger.info("Migrated {} anonymous views to user ID {}", updated, userId);
        }
    }

    /**
     * Record a view if not already recorded for this user/session in the last N
     * minutes. We use a unique constraint to prevent duplicates.
     */
    @Transactional
    public void recordView(Post post, Long userId, String sessionId, LocalDateTime viewedAt) {
        // Try to insert – if duplicate key, it will be ignored (thanks to unique
        // constraint)
        // Using native query to handle conflict gracefully.
        String sql = """
                     INSERT INTO tb_views (post_id, user_id, session_id, viewed_at)
                     VALUES (:postId, :userId, :sessionId, :viewedAt)
                     ON CONFLICT (post_id, user_id, session_id) DO NOTHING
                     """;
        entityManager.createNativeQuery(sql)
                     .setParameter("postId", post.getId())
                     .setParameter("userId", userId != null ? userId : null)
                     .setParameter("sessionId", sessionId)
                     .setParameter("viewedAt", viewedAt)
                     .executeUpdate();
    }
}