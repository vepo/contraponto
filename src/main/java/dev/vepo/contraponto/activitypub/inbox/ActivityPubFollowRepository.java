package dev.vepo.contraponto.activitypub.inbox;

import java.util.List;
import java.util.Optional;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import dev.vepo.contraponto.activitypub.ActivityPubFollowStatus;

@ApplicationScoped
public class ActivityPubFollowRepository {

    private final EntityManager entityManager;

    @Inject
    public ActivityPubFollowRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public ActivityPubFollow create(ActivityPubFollow follow) {
        entityManager.persist(follow);
        return follow;
    }

    public Optional<ActivityPubFollow> findById(long id) {
        return entityManager.createQuery("""
                                         SELECT f FROM ActivityPubFollow f
                                         JOIN FETCH f.localActor la
                                         JOIN FETCH la.user
                                         JOIN FETCH f.remoteActor
                                         WHERE f.id = :id
                                         """, ActivityPubFollow.class)
                            .setParameter("id", id)
                            .getResultStream()
                            .findFirst();
    }

    public Optional<ActivityPubFollow> findByLocalAndRemote(long localActorId, long remoteActorId) {
        return entityManager.createQuery("""
                                         SELECT f FROM ActivityPubFollow f
                                         JOIN FETCH f.localActor
                                         JOIN FETCH f.remoteActor
                                         WHERE f.localActor.id = :localActorId
                                           AND f.remoteActor.id = :remoteActorId
                                         """, ActivityPubFollow.class)
                            .setParameter("localActorId", localActorId)
                            .setParameter("remoteActorId", remoteActorId)
                            .getResultStream()
                            .findFirst();
    }

    public List<ActivityPubFollow> listAcceptedByLocalActor(long localActorId) {
        return entityManager.createQuery("""
                                         SELECT f FROM ActivityPubFollow f
                                         JOIN FETCH f.remoteActor
                                         WHERE f.localActor.id = :localActorId
                                           AND f.status = :status
                                         """, ActivityPubFollow.class)
                            .setParameter("localActorId", localActorId)
                            .setParameter("status", ActivityPubFollowStatus.ACCEPTED)
                            .getResultList();
    }

    public List<ActivityPubFollow> listPendingByLocalActor(long localActorId) {
        return entityManager.createQuery("""
                                         SELECT f FROM ActivityPubFollow f
                                         JOIN FETCH f.remoteActor
                                         WHERE f.localActor.id = :localActorId
                                           AND f.status = :status
                                         ORDER BY f.createdAt ASC
                                         """, ActivityPubFollow.class)
                            .setParameter("localActorId", localActorId)
                            .setParameter("status", ActivityPubFollowStatus.PENDING)
                            .getResultList();
    }

    @Transactional
    public ActivityPubFollow update(ActivityPubFollow follow) {
        return entityManager.merge(follow);
    }
}
