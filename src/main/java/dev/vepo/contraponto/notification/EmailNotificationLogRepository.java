package dev.vepo.contraponto.notification;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class EmailNotificationLogRepository {

    private final EntityManager entityManager;

    @Inject
    public EmailNotificationLogRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public boolean exists(long publicationId, long userId) {
        return entityManager.createQuery("""
                                         SELECT COUNT(l)
                                         FROM EmailNotificationLog l
                                         WHERE l.publication.id = :publicationId AND l.user.id = :userId
                                         """, Long.class)
                            .setParameter("publicationId", publicationId)
                            .setParameter("userId", userId)
                            .getSingleResult() > 0;
    }

    @Transactional
    public void persist(EmailNotificationLog log) {
        entityManager.persist(log);
    }
}
