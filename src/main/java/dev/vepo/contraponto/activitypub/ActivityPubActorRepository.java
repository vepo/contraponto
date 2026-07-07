package dev.vepo.contraponto.activitypub;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ActivityPubActorRepository {

    private final EntityManager entityManager;

    @Inject
    public ActivityPubActorRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public ActivityPubActor create(ActivityPubActor actor) {
        entityManager.persist(actor);
        return actor;
    }

    public Optional<ActivityPubActor> findById(long id) {
        return entityManager.createQuery("""
                                         SELECT a FROM ActivityPubActor a
                                         JOIN FETCH a.user
                                         WHERE a.id = :id
                                         """, ActivityPubActor.class)
                            .setParameter("id", id)
                            .getResultStream()
                            .findFirst();
    }

    public Optional<ActivityPubActor> findByPublicKeyId(String publicKeyId) {
        return entityManager.createQuery("""
                                         SELECT a FROM ActivityPubActor a
                                         JOIN FETCH a.user
                                         WHERE a.publicKeyId = :publicKeyId
                                         """, ActivityPubActor.class)
                            .setParameter("publicKeyId", publicKeyId)
                            .getResultStream()
                            .findFirst();
    }

    public Optional<ActivityPubActor> findByUserId(long userId) {
        return entityManager.createQuery("""
                                         SELECT a FROM ActivityPubActor a
                                         JOIN FETCH a.user
                                         WHERE a.user.id = :userId
                                         """, ActivityPubActor.class)
                            .setParameter("userId", userId)
                            .getResultStream()
                            .findFirst();
    }

    public Optional<ActivityPubActor> findEnabledByUsername(String username) {
        return entityManager.createQuery("""
                                         SELECT a FROM ActivityPubActor a
                                         JOIN FETCH a.user u
                                         WHERE u.username = :username
                                           AND a.federationEnabled = true
                                         """, ActivityPubActor.class)
                            .setParameter("username", username)
                            .getResultStream()
                            .findFirst();
    }

    public List<ActivityPubActor> listFederationEnabled() {
        return entityManager.createQuery("""
                                         SELECT a FROM ActivityPubActor a
                                         JOIN FETCH a.user
                                         WHERE a.federationEnabled = true
                                         """, ActivityPubActor.class)
                            .getResultList();
    }

    @Transactional
    public ActivityPubActor update(ActivityPubActor actor) {
        return entityManager.merge(actor);
    }
}
