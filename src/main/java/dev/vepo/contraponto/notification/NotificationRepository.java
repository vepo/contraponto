package dev.vepo.contraponto.notification;

import java.util.List;

import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class NotificationRepository {

    private final EntityManager entityManager;

    @Inject
    public NotificationRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public long countUnread(long recipientUserId) {
        return entityManager.createQuery("""
                                         SELECT COUNT(n)
                                         FROM Notification n
                                         WHERE n.recipient.id = :userId AND n.read = FALSE
                                         """, Long.class)
                            .setParameter("userId", recipientUserId)
                            .getSingleResult();
    }

    @Transactional
    public Notification create(Notification notification) {
        entityManager.persist(notification);
        return notification;
    }

    public Page<Notification> findPage(long recipientUserId, PageQuery query) {
        long total = entityManager.createQuery("""
                                               SELECT COUNT(n)
                                               FROM Notification n
                                               WHERE n.recipient.id = :userId
                                               """, Long.class)
                                  .setParameter("userId", recipientUserId)
                                  .getSingleResult();

        List<Notification> data = entityManager.createQuery("""
                                                            SELECT n FROM Notification n
                                                            JOIN FETCH n.blog b
                                                            LEFT JOIN FETCH b.owner
                                                            LEFT JOIN FETCH n.post p
                                                            LEFT JOIN FETCH n.actor
                                                            WHERE n.recipient.id = :userId
                                                            ORDER BY n.createdAt DESC
                                                            """, Notification.class)
                                               .setParameter("userId", recipientUserId)
                                               .setFirstResult(query.skip())
                                               .setMaxResults(query.limit())
                                               .getResultList();

        return new Page<>(data, query.page(), query.limit(), total);
    }

    @Transactional
    public int markAllRead(long recipientUserId) {
        return entityManager.createQuery("""
                                         UPDATE Notification n
                                         SET n.read = TRUE
                                         WHERE n.recipient.id = :userId AND n.read = FALSE
                                         """)
                            .setParameter("userId", recipientUserId)
                            .executeUpdate();
    }
}
