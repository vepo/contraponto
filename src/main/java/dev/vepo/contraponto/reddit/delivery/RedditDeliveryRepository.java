package dev.vepo.contraponto.reddit.delivery;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class RedditDeliveryRepository {

    private static final int BATCH_SIZE = 20;

    private final EntityManager entityManager;

    @Inject
    public RedditDeliveryRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public RedditDelivery create(RedditDelivery delivery) {
        entityManager.persist(delivery);
        return delivery;
    }

    public List<RedditDelivery> findPendingReady(LocalDateTime now) {
        return entityManager.createQuery("""
                                         SELECT d FROM RedditDelivery d
                                         JOIN FETCH d.post p
                                         JOIN FETCH p.blog
                                         JOIN FETCH d.user
                                         WHERE d.status = :status
                                           AND (d.nextRetryAt IS NULL OR d.nextRetryAt <= :now)
                                         ORDER BY d.createdAt ASC
                                         """, RedditDelivery.class)
                            .setParameter("status", RedditDeliveryStatus.PENDING)
                            .setParameter("now", now)
                            .setMaxResults(BATCH_SIZE)
                            .getResultList();
    }

    @Transactional
    public RedditDelivery update(RedditDelivery delivery) {
        return entityManager.merge(delivery);
    }
}
