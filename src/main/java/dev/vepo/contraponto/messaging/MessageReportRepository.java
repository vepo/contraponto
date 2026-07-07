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
public class MessageReportRepository {

    private final EntityManager entityManager;

    @Inject
    public MessageReportRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public int deleteOlderThan(LocalDateTime cutoff) {
        return entityManager.createQuery("""
                                         DELETE FROM MessageReport r
                                         WHERE r.createdAt < :cutoff
                                         """)
                            .setParameter("cutoff", cutoff)
                            .executeUpdate();
    }

    public Optional<MessageReport> findById(long id) {
        return Optional.ofNullable(entityManager.find(MessageReport.class, id));
    }

    public Optional<MessageReport> findByThreadAndReporter(long threadId, long reporterUserId) {
        return entityManager.createQuery("""
                                         SELECT r
                                         FROM MessageReport r
                                         WHERE r.thread.id = :threadId AND r.reporter.id = :reporterId
                                         """, MessageReport.class)
                            .setParameter("threadId", threadId)
                            .setParameter("reporterId", reporterUserId)
                            .getResultStream()
                            .findFirst();
    }

    public Page<MessageReportRow> findPageByStatus(MessageReportStatus status, PageQuery query) {
        long total = entityManager.createQuery("""
                                               SELECT COUNT(r)
                                               FROM MessageReport r
                                               WHERE r.status = :status
                                               """, Long.class)
                                  .setParameter("status", status)
                                  .getSingleResult();

        var rows = entityManager.createQuery("""
                                             SELECT new dev.vepo.contraponto.messaging.MessageReportRow(
                                                 r.id, t.id, t.title, u.username, u.name, r.status, r.createdAt)
                                             FROM MessageReport r
                                             JOIN r.thread t
                                             JOIN r.reporter u
                                             WHERE r.status = :status
                                             ORDER BY r.createdAt DESC
                                             """, MessageReportRow.class)
                                .setParameter("status", status)
                                .setFirstResult(query.skip())
                                .setMaxResults(query.maxResults())
                                .getResultList();
        return new Page<>(rows, query.page(), query.limit(), total);
    }

    public Page<MessageReportRow> findPendingPage(PageQuery query) {
        return findPageByStatus(MessageReportStatus.PENDING, query);
    }

    public List<ThreadMessage> findThreadMessagesForReport(long threadId) {
        return entityManager.createQuery("""
                                         SELECT m
                                         FROM ThreadMessage m
                                         WHERE m.thread.id = :threadId
                                         ORDER BY m.createdAt ASC
                                         """, ThreadMessage.class)
                            .setParameter("threadId", threadId)
                            .getResultList();
    }

    @Transactional
    public MessageReport save(MessageReport report) {
        if (report.getId() == null) {
            entityManager.persist(report);
        } else {
            report = entityManager.merge(report);
        }
        return report;
    }
}
