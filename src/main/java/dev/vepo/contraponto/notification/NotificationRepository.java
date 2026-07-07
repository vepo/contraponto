package dev.vepo.contraponto.notification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class NotificationRepository {

    private static final String PARAM_USER_ID = "userId";

    private final EntityManager entityManager;

    @Inject
    public NotificationRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Map<LocalDate, Long> countDailyByBlogAndType(long blogId,
                                                        long recipientUserId,
                                                        NotificationType type,
                                                        LocalDateTime startInclusive,
                                                        LocalDateTime endExclusive) {
        // Native: CAST(created_at AS date) for daily GROUP BY is database-specific.
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                                                              SELECT CAST(n.created_at AS date), COUNT(*)
                                                              FROM tb_notifications n
                                                              WHERE n.blog_id = :blogId
                                                                AND n.recipient_user_id = :recipientUserId
                                                                AND n.type = :type
                                                                AND n.created_at >= :start
                                                                AND n.created_at < :end
                                                              GROUP BY CAST(n.created_at AS date)
                                                              ORDER BY 1
                                                              """)
                                           .setParameter("blogId", blogId)
                                           .setParameter("recipientUserId", recipientUserId)
                                           .setParameter("type", type.name())
                                           .setParameter("start", startInclusive)
                                           .setParameter("end", endExclusive)
                                           .getResultList();

        Map<LocalDate, Long> counts = new LinkedHashMap<>();
        for (Object[] row : rows) {
            LocalDate day = row[0] instanceof java.sql.Date sqlDate ? sqlDate.toLocalDate() : LocalDate.parse(row[0].toString());
            counts.put(day, ((Number) row[1]).longValue());
        }
        return counts;
    }

    public long countUnread(long recipientUserId) {
        return entityManager.createQuery("""
                                         SELECT COUNT(n)
                                         FROM Notification n
                                         WHERE n.recipient.id = :userId AND n.read = FALSE
                                         """, Long.class)
                            .setParameter(PARAM_USER_ID, recipientUserId)
                            .getSingleResult();
    }

    @Transactional
    public Notification create(Notification notification) {
        entityManager.persist(notification);
        return notification;
    }

    @Transactional
    public int deleteExpiredRead(LocalDateTime readCutoffExclusive) {
        return entityManager.createQuery("""
                                         DELETE FROM Notification n
                                         WHERE n.read = TRUE
                                           AND n.readAt IS NOT NULL
                                           AND n.readAt < :cutoff
                                         """)
                            .setParameter("cutoff", readCutoffExclusive)
                            .executeUpdate();
    }

    @Transactional
    public int deleteExpiredUnread(LocalDateTime createdCutoffExclusive) {
        return entityManager.createQuery("""
                                         DELETE FROM Notification n
                                         WHERE n.read = FALSE
                                           AND n.createdAt < :cutoff
                                         """)
                            .setParameter("cutoff", createdCutoffExclusive)
                            .executeUpdate();
    }

    public Page<Notification> findPage(long recipientUserId, PageQuery query) {
        long total = entityManager.createQuery("""
                                               SELECT COUNT(n)
                                               FROM Notification n
                                               WHERE n.recipient.id = :userId
                                               """, Long.class)
                                  .setParameter(PARAM_USER_ID, recipientUserId)
                                  .getSingleResult();

        List<Notification> data = entityManager.createQuery("""
                                                            SELECT DISTINCT n FROM Notification n
                                                            LEFT JOIN FETCH n.blog b
                                                            LEFT JOIN FETCH b.owner
                                                            LEFT JOIN FETCH n.messageThread
                                                            LEFT JOIN FETCH n.post p
                                                            LEFT JOIN FETCH n.actor
                                                            LEFT JOIN FETCH n.gitSyncRun
                                                            WHERE n.recipient.id = :userId
                                                            ORDER BY n.createdAt DESC
                                                            """, Notification.class)
                                               .setParameter(PARAM_USER_ID, recipientUserId)
                                               .setFirstResult(query.skip())
                                               .setMaxResults(query.limit())
                                               .getResultList();

        return new Page<>(data, query.page(), query.limit(), total);
    }

    public List<Notification> findUnreadRecent(long recipientUserId, int limit) {
        return entityManager.createQuery("""
                                         SELECT DISTINCT n FROM Notification n
                                         LEFT JOIN FETCH n.blog b
                                         LEFT JOIN FETCH b.owner
                                         LEFT JOIN FETCH n.messageThread
                                         LEFT JOIN FETCH n.post p
                                         LEFT JOIN FETCH n.actor
                                         LEFT JOIN FETCH n.gitSyncRun
                                         WHERE n.recipient.id = :userId AND n.read = FALSE
                                         ORDER BY n.createdAt DESC
                                         """, Notification.class)
                            .setParameter(PARAM_USER_ID, recipientUserId)
                            .setMaxResults(limit)
                            .getResultList();
    }

    @Transactional
    public int markAllRead(long recipientUserId) {
        var now = LocalDateTime.now(java.time.ZoneId.systemDefault());
        return entityManager.createQuery("""
                                         UPDATE Notification n
                                         SET n.read = TRUE, n.readAt = :readAt
                                         WHERE n.recipient.id = :userId AND n.read = FALSE
                                         """)
                            .setParameter(PARAM_USER_ID, recipientUserId)
                            .setParameter("readAt", now)
                            .executeUpdate();
    }

    @Transactional
    public boolean markRead(long notificationId, long recipientUserId) {
        var now = LocalDateTime.now(java.time.ZoneId.systemDefault());
        int updated = entityManager.createQuery("""
                                                UPDATE Notification n
                                                SET n.read = TRUE, n.readAt = :readAt
                                                WHERE n.id = :id
                                                  AND n.recipient.id = :userId
                                                  AND n.read = FALSE
                                                """)
                                   .setParameter("id", notificationId)
                                   .setParameter(PARAM_USER_ID, recipientUserId)
                                   .setParameter("readAt", now)
                                   .executeUpdate();
        return updated > 0;
    }
}
