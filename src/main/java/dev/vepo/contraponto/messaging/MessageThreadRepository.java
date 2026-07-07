package dev.vepo.contraponto.messaging;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class MessageThreadRepository {

    private final EntityManager entityManager;

    @Inject
    public MessageThreadRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public long countNewThreadsSince(long initiatorUserId, LocalDateTime since) {
        return entityManager.createQuery("""
                                         SELECT COUNT(t)
                                         FROM MessageThread t
                                         WHERE t.initiator.id = :userId
                                           AND t.createdAt >= :since
                                         """, Long.class)
                            .setParameter("userId", initiatorUserId)
                            .setParameter("since", since)
                            .getSingleResult();
    }

    public Optional<MessageThread> findById(long id) {
        return Optional.ofNullable(entityManager.find(MessageThread.class, id));
    }

    public Optional<MessageThread> findByIdForParticipant(long id, long userId) {
        return entityManager.createQuery("""
                                         SELECT t
                                         FROM MessageThread t
                                         WHERE t.id = :id
                                           AND (t.initiator.id = :userId OR t.recipient.id = :userId)
                                         """, MessageThread.class)
                            .setParameter("id", id)
                            .setParameter("userId", userId)
                            .getResultStream()
                            .findFirst();
    }

    public Page<MessageThreadRow> findMailboxPage(long userId, MessageThreadStatus tabStatus, PageQuery query) {
        // Native: latest message preview per thread via correlated subquery.
        long total = ((Number) entityManager.createNativeQuery("""
                                                               SELECT COUNT(*)
                                                               FROM tb_message_threads t
                                                               WHERE (t.initiator_user_id = :userId OR t.recipient_user_id = :userId)
                                                                 AND t.status = :status
                                                               """)
                                            .setParameter("userId", userId)
                                            .setParameter("status", tabStatus.name())
                                            .getSingleResult()).longValue();

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                                                              SELECT t.id,
                                                                     t.title,
                                                                     CASE WHEN t.initiator_user_id = :userId THEN ru.username ELSE iu.username END,
                                                                     CASE WHEN t.initiator_user_id = :userId THEN ru.name ELSE iu.name END,
                                                                     COALESCE((
                                                                         SELECT LEFT(m.body, 120)
                                                                         FROM tb_thread_messages m
                                                                         WHERE m.thread_id = t.id
                                                                         ORDER BY m.created_at DESC
                                                                         LIMIT 1
                                                                     ), ''),
                                                                     t.created_at,
                                                                     CASE WHEN p.last_read_message_id IS NULL THEN TRUE
                                                                          WHEN (
                                                                              SELECT m2.id FROM tb_thread_messages m2
                                                                              WHERE m2.thread_id = t.id
                                                                              ORDER BY m2.created_at DESC LIMIT 1
                                                                          ) <> p.last_read_message_id THEN TRUE
                                                                          ELSE FALSE END
                                                              FROM tb_message_threads t
                                                              JOIN tb_users iu ON iu.id = t.initiator_user_id
                                                              JOIN tb_users ru ON ru.id = t.recipient_user_id
                                                              JOIN tb_message_thread_participants p ON p.thread_id = t.id AND p.user_id = :userId
                                                              WHERE (t.initiator_user_id = :userId OR t.recipient_user_id = :userId)
                                                                AND t.status = :status
                                                              ORDER BY t.created_at DESC
                                                              """)
                                           .setParameter("userId", userId)
                                           .setParameter("status", tabStatus.name())
                                           .setFirstResult(query.skip())
                                           .setMaxResults(query.maxResults())
                                           .getResultList();

        var data = rows.stream()
                       .map(row -> new MessageThreadRow(((Number) row[0]).longValue(),
                                                        (String) row[1],
                                                        (String) row[2],
                                                        (String) row[3],
                                                        (String) row[4],
                                                        row[5] instanceof java.sql.Timestamp ts ? ts.toLocalDateTime()
                                                                                                : LocalDateTime.parse(row[5].toString()),
                                                        Boolean.TRUE.equals(row[6])))
                       .toList();
        return new Page<>(data, query.page(), query.limit(), total);
    }

    public List<MessageThread> findOpenBetweenUsers(long userIdA, long userIdB) {
        return entityManager.createQuery("""
                                         SELECT t
                                         FROM MessageThread t
                                         WHERE t.status = :open
                                           AND (
                                             (t.initiator.id = :a AND t.recipient.id = :b)
                                             OR (t.initiator.id = :b AND t.recipient.id = :a)
                                           )
                                         """, MessageThread.class)
                            .setParameter("open", MessageThreadStatus.OPEN)
                            .setParameter("a", userIdA)
                            .setParameter("b", userIdB)
                            .getResultList();
    }

    @Transactional
    public MessageThread save(MessageThread thread) {
        if (thread.getId() == null) {
            entityManager.persist(thread);
        } else {
            thread = entityManager.merge(thread);
        }
        return thread;
    }
}
