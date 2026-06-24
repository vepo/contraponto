package dev.vepo.contraponto.readinglist;

import java.time.LocalDateTime;
import java.util.Optional;

import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ReadingListRepository {

    private static final String PARAM_USER_ID = "userId";

    private final EntityManager entityManager;

    @Inject
    public ReadingListRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public long countByUser(long userId) {
        return entityManager.createQuery("""
                                         SELECT COUNT(i) FROM ReadingListItem i
                                         WHERE i.user.id = :userId
                                         """, Long.class)
                            .setParameter(PARAM_USER_ID, userId)
                            .getSingleResult();
    }

    public long countSavesSince(long userId, LocalDateTime since) {
        return entityManager.createQuery("""
                                         SELECT COUNT(i) FROM ReadingListItem i
                                         WHERE i.user.id = :userId AND i.savedAt >= :since
                                         """, Long.class)
                            .setParameter(PARAM_USER_ID, userId)
                            .setParameter("since", since)
                            .getSingleResult();
    }

    public long countUnread(long userId) {
        return entityManager.createQuery("""
                                         SELECT COUNT(i) FROM ReadingListItem i
                                         WHERE i.user.id = :userId AND i.readAt IS NULL
                                         """, Long.class)
                            .setParameter(PARAM_USER_ID, userId)
                            .getSingleResult();
    }

    @Transactional
    public void delete(ReadingListItem item) {
        ReadingListItem managed = entityManager.contains(item) ? item : entityManager.merge(item);
        entityManager.remove(managed);
    }

    public Page<ReadingListRow> findAllPage(long userId, PageQuery query) {
        long total = entityManager.createQuery("""
                                               SELECT COUNT(i) FROM ReadingListItem i
                                               WHERE i.user.id = :userId
                                               """, Long.class)
                                  .setParameter(PARAM_USER_ID, userId)
                                  .getSingleResult();
        var rows = entityManager.createQuery("""
                                             SELECT new dev.vepo.contraponto.readinglist.ReadingListRow(
                                                 i.id, p.id, p.title, p.slug, b.slug, b.main,
                                                 o.username, o.name, b.name, i.savedAt, i.readAt,
                                                 p.published, b.active)
                                             FROM ReadingListItem i
                                             JOIN i.post p
                                             JOIN p.blog b
                                             JOIN b.owner o
                                             WHERE i.user.id = :userId
                                             ORDER BY i.readAt DESC NULLS FIRST, i.savedAt DESC
                                             """, ReadingListRow.class)
                                .setParameter(PARAM_USER_ID, userId)
                                .setFirstResult(query.skip())
                                .setMaxResults(query.maxResults())
                                .getResultList();
        return new Page<>(rows, query.page(), query.limit(), total);
    }

    public Optional<ReadingListItem> findByIdForUser(long itemId, long userId) {
        return entityManager.createQuery("""
                                         SELECT i FROM ReadingListItem i
                                         JOIN FETCH i.post p
                                         JOIN FETCH p.blog b
                                         JOIN FETCH b.owner
                                         WHERE i.id = :itemId AND i.user.id = :userId
                                         """, ReadingListItem.class)
                            .setParameter("itemId", itemId)
                            .setParameter(PARAM_USER_ID, userId)
                            .getResultStream()
                            .findFirst();
    }

    public Optional<ReadingListItem> findByUserAndPost(long userId, long postId) {
        return entityManager.createQuery("""
                                         SELECT i FROM ReadingListItem i
                                         JOIN FETCH i.post p
                                         JOIN FETCH p.blog b
                                         JOIN FETCH b.owner
                                         WHERE i.user.id = :userId AND i.post.id = :postId
                                         """, ReadingListItem.class)
                            .setParameter(PARAM_USER_ID, userId)
                            .setParameter("postId", postId)
                            .getResultStream()
                            .findFirst();
    }

    public Page<ReadingListRow> findUnreadPage(long userId, PageQuery query) {
        long total = entityManager.createQuery("""
                                               SELECT COUNT(i) FROM ReadingListItem i
                                               WHERE i.user.id = :userId AND i.readAt IS NULL
                                               """, Long.class)
                                  .setParameter(PARAM_USER_ID, userId)
                                  .getSingleResult();
        var rows = entityManager.createQuery("""
                                             SELECT new dev.vepo.contraponto.readinglist.ReadingListRow(
                                                 i.id, p.id, p.title, p.slug, b.slug, b.main,
                                                 o.username, o.name, b.name, i.savedAt, i.readAt,
                                                 p.published, b.active)
                                             FROM ReadingListItem i
                                             JOIN i.post p
                                             JOIN p.blog b
                                             JOIN b.owner o
                                             WHERE i.user.id = :userId AND i.readAt IS NULL
                                             ORDER BY i.savedAt ASC
                                             """, ReadingListRow.class)
                                .setParameter(PARAM_USER_ID, userId)
                                .setFirstResult(query.skip())
                                .setMaxResults(query.maxResults())
                                .getResultList();
        return new Page<>(rows, query.page(), query.limit(), total);
    }

    @Transactional
    public ReadingListItem save(ReadingListItem item) {
        if (item.getId() == null) {
            entityManager.persist(item);
            return item;
        }
        return entityManager.merge(item);
    }
}
