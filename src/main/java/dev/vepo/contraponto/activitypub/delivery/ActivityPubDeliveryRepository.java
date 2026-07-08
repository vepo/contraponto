package dev.vepo.contraponto.activitypub.delivery;

import java.time.LocalDateTime;
import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import dev.vepo.contraponto.activitypub.ActivityPubDeliveryStatus;

@ApplicationScoped
public class ActivityPubDeliveryRepository {

    private static final int BATCH_SIZE = 20;

    private final EntityManager entityManager;

    @Inject
    public ActivityPubDeliveryRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public long countPendingForActor(long localActorId) {
        return entityManager.createQuery("""
                                         SELECT COUNT(d)
                                         FROM ActivityPubDelivery d
                                         WHERE d.localActor.id = :localActorId
                                           AND d.status = :status
                                         """, Long.class)
                            .setParameter("localActorId", localActorId)
                            .setParameter("status", ActivityPubDeliveryStatus.PENDING)
                            .getSingleResult();
    }

    @Transactional
    public ActivityPubDelivery create(ActivityPubDelivery delivery) {
        entityManager.persist(delivery);
        return delivery;
    }

    public List<ActivityPubDelivery> findPendingReady(LocalDateTime now) {
        return entityManager.createQuery("""
                                         SELECT d FROM ActivityPubDelivery d
                                         JOIN FETCH d.localActor la
                                         JOIN FETCH la.user
                                         WHERE d.status = :status
                                           AND (d.nextRetryAt IS NULL OR d.nextRetryAt <= :now)
                                         ORDER BY d.createdAt ASC
                                         """, ActivityPubDelivery.class)
                            .setParameter("status", ActivityPubDeliveryStatus.PENDING)
                            .setParameter("now", now)
                            .setMaxResults(BATCH_SIZE)
                            .getResultList();
    }

    @Transactional
    public ActivityPubDelivery update(ActivityPubDelivery delivery) {
        return entityManager.merge(delivery);
    }
}
