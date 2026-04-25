package dev.vepo.contraponto.view;

import java.time.LocalDateTime;
import java.util.List;

import dev.vepo.contraponto.post.Post;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ViewRepository {

    @Inject
    EntityManager entityManager;

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

    public long countByPost(Post post) {
        return entityManager.createQuery("SELECT COUNT(v) FROM View v WHERE v.post = :post", Long.class)
                            .setParameter("post", post)
                            .getSingleResult();
    }

    public List<Object[]> getViewCountsForPosts(List<Post> posts) {
        if (posts.isEmpty())
            return List.of();
        return entityManager.createQuery("""
                                         SELECT v.post.id, COUNT(v)
                                         FROM View v
                                         WHERE v.post IN :posts
                                         GROUP BY v.post.id
                                         """, Object[].class)
                            .setParameter("posts", posts)
                            .getResultList();
    }
}