package dev.vepo.contraponto.post;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class PostPublicationRepository {

    private final EntityManager entityManager;

    @Inject
    public PostPublicationRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<PostPublication> findById(long id) {
        return Optional.ofNullable(entityManager.find(PostPublication.class, id));
    }

    public List<PostPublication> findByPostIdOrderByVersionDesc(long postId) {
        return entityManager.createQuery("""
                                         SELECT p FROM PostPublication p
                                         LEFT JOIN FETCH p.tags
                                         LEFT JOIN FETCH p.cover
                                         WHERE p.post.id = :postId
                                         ORDER BY p.version DESC
                                         """, PostPublication.class)
                            .setParameter("postId", postId)
                            .getResultList();
    }

    public Optional<PostPublication> findLatestByPostId(long postId) {
        return findByPostIdOrderByVersionDesc(postId).stream().findFirst();
    }

    public Optional<Integer> findMaxVersion(long postId) {
        Integer max = entityManager.createQuery("""
                                                SELECT MAX(p.version) FROM PostPublication p
                                                WHERE p.post.id = :postId
                                                """, Integer.class)
                                   .setParameter("postId", postId)
                                   .getSingleResult();
        return Optional.ofNullable(max);
    }

    @Transactional
    public PostPublication save(PostPublication publication) {
        entityManager.persist(publication);
        return publication;
    }
}
