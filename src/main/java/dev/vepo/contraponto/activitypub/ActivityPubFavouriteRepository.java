package dev.vepo.contraponto.activitypub;

import java.util.List;
import java.util.Optional;

import dev.vepo.contraponto.post.Post;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ActivityPubFavouriteRepository {

    private final EntityManager entityManager;

    @Inject
    public ActivityPubFavouriteRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public long countByPostId(long postId) {
        return entityManager.createQuery("""
                                         SELECT COUNT(f) FROM ActivityPubFavourite f
                                         WHERE f.post.id = :postId
                                         """, Long.class)
                            .setParameter("postId", postId)
                            .getSingleResult();
    }

    @Transactional
    public ActivityPubFavourite create(ActivityPubFavourite favourite) {
        entityManager.persist(favourite);
        return favourite;
    }

    @Transactional
    public void delete(ActivityPubFavourite favourite) {
        entityManager.remove(entityManager.contains(favourite) ? favourite : entityManager.merge(favourite));
    }

    public Optional<ActivityPubFavourite> findByPostAndRemote(long postId, long remoteActorId) {
        return entityManager.createQuery("""
                                         SELECT f FROM ActivityPubFavourite f
                                         WHERE f.post.id = :postId
                                           AND f.remoteActor.id = :remoteActorId
                                         """, ActivityPubFavourite.class)
                            .setParameter("postId", postId)
                            .setParameter("remoteActorId", remoteActorId)
                            .getResultStream()
                            .findFirst();
    }

    public List<ActivityPubFavourite> listByPostIdWithRemoteActor(long postId) {
        return entityManager.createQuery("""
                                         SELECT f FROM ActivityPubFavourite f
                                         JOIN FETCH f.remoteActor
                                         WHERE f.post.id = :postId
                                         ORDER BY f.createdAt ASC
                                         """, ActivityPubFavourite.class)
                            .setParameter("postId", postId)
                            .getResultList();
    }

    @Transactional
    public ActivityPubFavourite saveLike(Post post, ActivityPubRemoteActor remoteActor, String likeActivityId) {
        return findByPostAndRemote(post.getId(), remoteActor.getId())
                                                                     .orElseGet(() -> create(new ActivityPubFavourite(post, remoteActor, likeActivityId)));
    }
}
