package dev.vepo.contraponto.reaction;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class PostReactionRepository {

    private final EntityManager entityManager;

    @Inject
    public PostReactionRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Map<ReactionType, Long> countsByPost(long postId) {
        List<Object[]> rows = entityManager.createQuery("""
                                                        SELECT r.reactionType, COUNT(r)
                                                        FROM PostReaction r
                                                        WHERE r.post.id = :postId
                                                        GROUP BY r.reactionType
                                                        """, Object[].class)
                                           .setParameter("postId", postId)
                                           .getResultList();
        Map<ReactionType, Long> counts = new EnumMap<>(ReactionType.class);
        for (Object[] row : rows) {
            counts.put((ReactionType) row[0], (Long) row[1]);
        }
        return counts;
    }

    @Transactional
    public void delete(PostReaction reaction) {
        entityManager.remove(entityManager.contains(reaction) ? reaction : entityManager.merge(reaction));
    }

    public Optional<PostReaction> find(long postId, long userId, ReactionType type) {
        return entityManager.createQuery("""
                                         SELECT r FROM PostReaction r
                                         WHERE r.post.id = :postId AND r.user.id = :userId AND r.reactionType = :type
                                         """, PostReaction.class)
                            .setParameter("postId", postId)
                            .setParameter("userId", userId)
                            .setParameter("type", type)
                            .getResultStream()
                            .findFirst();
    }

    public Set<ReactionType> findActiveTypes(long postId, long userId) {
        List<ReactionType> types = entityManager.createQuery("""
                                                             SELECT r.reactionType FROM PostReaction r
                                                             WHERE r.post.id = :postId AND r.user.id = :userId
                                                             """, ReactionType.class)
                                                .setParameter("postId", postId)
                                                .setParameter("userId", userId)
                                                .getResultList();
        return new HashSet<>(types);
    }

    @Transactional
    public PostReaction save(PostReaction reaction) {
        entityManager.persist(reaction);
        return reaction;
    }
}
