package dev.vepo.contraponto.messaging;

import java.util.Optional;

import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class UserBlockRepository {

    private final EntityManager entityManager;

    @Inject
    public UserBlockRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public void delete(UserBlock block) {
        UserBlock managed = entityManager.contains(block) ? block : entityManager.merge(block);
        entityManager.remove(managed);
    }

    public Page<UserBlockRow> findBlockedPage(long blockerUserId, PageQuery query) {
        long total = entityManager.createQuery("""
                                               SELECT COUNT(b)
                                               FROM UserBlock b
                                               WHERE b.blocker.id = :blockerId
                                               """, Long.class)
                                  .setParameter("blockerId", blockerUserId)
                                  .getSingleResult();

        var rows = entityManager.createQuery("""
                                             SELECT new dev.vepo.contraponto.messaging.UserBlockRow(
                                                 b.id, u.id, u.username, u.name, b.reason, b.createdAt)
                                             FROM UserBlock b
                                             JOIN b.blocked u
                                             WHERE b.blocker.id = :blockerId
                                             ORDER BY b.createdAt DESC
                                             """, UserBlockRow.class)
                                .setParameter("blockerId", blockerUserId)
                                .setFirstResult(query.skip())
                                .setMaxResults(query.maxResults())
                                .getResultList();
        return new Page<>(rows, query.page(), query.limit(), total);
    }

    public Optional<UserBlock> findByBlockerAndBlocked(long blockerUserId, long blockedUserId) {
        return entityManager.createQuery("""
                                         SELECT b
                                         FROM UserBlock b
                                         WHERE b.blocker.id = :blockerId AND b.blocked.id = :blockedId
                                         """, UserBlock.class)
                            .setParameter("blockerId", blockerUserId)
                            .setParameter("blockedId", blockedUserId)
                            .getResultStream()
                            .findFirst();
    }

    public boolean isBlockedEitherDirection(long userIdA, long userIdB) {
        return entityManager.createQuery("""
                                         SELECT COUNT(b)
                                         FROM UserBlock b
                                         WHERE (b.blocker.id = :a AND b.blocked.id = :b)
                                            OR (b.blocker.id = :b AND b.blocked.id = :a)
                                         """, Long.class)
                            .setParameter("a", userIdA)
                            .setParameter("b", userIdB)
                            .getSingleResult() > 0;
    }

    @Transactional
    public UserBlock save(UserBlock block) {
        entityManager.persist(block);
        return block;
    }
}
