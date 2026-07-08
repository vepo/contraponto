package dev.vepo.contraponto.activitypub.remote;

import java.util.Optional;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ActivityPubRemoteActorRepository {

    private final EntityManager entityManager;

    @Inject
    public ActivityPubRemoteActorRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public ActivityPubRemoteActor create(ActivityPubRemoteActor remoteActor) {
        entityManager.persist(remoteActor);
        return remoteActor;
    }

    public Optional<ActivityPubRemoteActor> findByActorId(String actorId) {
        return entityManager.createQuery("""
                                         SELECT r FROM ActivityPubRemoteActor r
                                         WHERE r.actorId = :actorId
                                         """, ActivityPubRemoteActor.class)
                            .setParameter("actorId", actorId)
                            .getResultStream()
                            .findFirst();
    }

    public Optional<ActivityPubRemoteActor> findById(long id) {
        return entityManager.createQuery("""
                                         SELECT r FROM ActivityPubRemoteActor r
                                         WHERE r.id = :id
                                         """, ActivityPubRemoteActor.class)
                            .setParameter("id", id)
                            .getResultStream()
                            .findFirst();
    }

    public Optional<ActivityPubRemoteActor> findByPublicKeyId(String publicKeyId) {
        return entityManager.createQuery("""
                                         SELECT r FROM ActivityPubRemoteActor r
                                         WHERE r.publicKeyId = :publicKeyId
                                         """, ActivityPubRemoteActor.class)
                            .setParameter("publicKeyId", publicKeyId)
                            .getResultStream()
                            .findFirst();
    }

    @Transactional
    public ActivityPubRemoteActor save(ActivityPubRemoteActor remoteActor) {
        if (remoteActor.getId() == null) {
            entityManager.persist(remoteActor);
            return remoteActor;
        }
        return entityManager.merge(remoteActor);
    }
}
