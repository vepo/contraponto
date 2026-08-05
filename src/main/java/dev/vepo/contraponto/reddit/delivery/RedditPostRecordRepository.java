package dev.vepo.contraponto.reddit.delivery;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class RedditPostRecordRepository {

    private final EntityManager entityManager;

    @Inject
    public RedditPostRecordRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public RedditPostRecord create(RedditPostRecord record) {
        entityManager.persist(record);
        return record;
    }

    @Transactional
    public void delete(RedditPostRecord record) {
        entityManager.remove(entityManager.contains(record) ? record : entityManager.merge(record));
    }

    public Optional<RedditPostRecord> findByPostId(long postId) {
        return entityManager.createQuery("""
                                         SELECT r FROM RedditPostRecord r
                                         WHERE r.post.id = :postId
                                         """, RedditPostRecord.class)
                            .setParameter("postId", postId)
                            .getResultStream()
                            .findFirst();
    }
}
