package dev.vepo.contraponto.highlight;

import java.util.List;
import java.util.Optional;

import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class PostTextHighlightRepository {

    private final EntityManager entityManager;

    @Inject
    public PostTextHighlightRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public long countByUserAndPost(long userId, long postId) {
        return entityManager.createQuery("""
                                         SELECT COUNT(h) FROM PostTextHighlight h
                                         WHERE h.user.id = :userId AND h.post.id = :postId
                                         """, Long.class)
                            .setParameter("userId", userId)
                            .setParameter("postId", postId)
                            .getSingleResult();
    }

    public long countDistinctReadersInCluster(long postId, String anchorClusterHash) {
        return entityManager.createQuery("""
                                         SELECT COUNT(DISTINCT h.user.id) FROM PostTextHighlight h
                                         WHERE h.post.id = :postId AND h.anchorClusterHash = :hash
                                         """, Long.class)
                            .setParameter("postId", postId)
                            .setParameter("hash", anchorClusterHash)
                            .getSingleResult();
    }

    @Transactional
    public void delete(PostTextHighlight highlight) {
        PostTextHighlight managed = entityManager.contains(highlight)
                                                                      ? highlight
                                                                      : entityManager.merge(highlight);
        entityManager.remove(managed);
    }

    public Optional<PostTextHighlight> findById(long id) {
        return entityManager.createQuery("""
                                         SELECT h FROM PostTextHighlight h
                                         JOIN FETCH h.post p
                                         JOIN FETCH p.blog b
                                         JOIN FETCH b.owner
                                         JOIN FETCH h.user
                                         JOIN FETCH h.publication
                                         WHERE h.id = :id
                                         """, PostTextHighlight.class)
                            .setParameter("id", id)
                            .getResultStream()
                            .findFirst();
    }

    public List<PostTextHighlight> findByPostForUser(long postId, long userId) {
        return entityManager.createQuery("""
                                         SELECT h FROM PostTextHighlight h
                                         JOIN FETCH h.user
                                         WHERE h.post.id = :postId AND h.user.id = :userId
                                         ORDER BY h.createdAt ASC
                                         """, PostTextHighlight.class)
                            .setParameter("postId", postId)
                            .setParameter("userId", userId)
                            .getResultList();
    }

    public Optional<PostTextHighlight> findByUserPostAndCluster(long userId, long postId, String anchorClusterHash) {
        return entityManager.createQuery("""
                                         SELECT h FROM PostTextHighlight h
                                         JOIN FETCH h.post p
                                         JOIN FETCH p.blog b
                                         JOIN FETCH b.owner
                                         JOIN FETCH h.user
                                         WHERE h.user.id = :userId AND h.post.id = :postId
                                           AND h.anchorClusterHash = :hash
                                         """, PostTextHighlight.class)
                            .setParameter("userId", userId)
                            .setParameter("postId", postId)
                            .setParameter("hash", anchorClusterHash)
                            .getResultStream()
                            .findFirst();
    }

    public Optional<PostTextHighlight> findCanonicalInCluster(long postId, String anchorClusterHash) {
        return entityManager.createQuery("""
                                         SELECT h FROM PostTextHighlight h
                                         JOIN FETCH h.publication
                                         WHERE h.post.id = :postId AND h.anchorClusterHash = :hash
                                         ORDER BY h.createdAt ASC
                                         """, PostTextHighlight.class)
                            .setParameter("postId", postId)
                            .setParameter("hash", anchorClusterHash)
                            .setMaxResults(1)
                            .getResultStream()
                            .findFirst();
    }

    public Page<HighlightLibraryRow> findLibraryForUser(long userId, PageQuery query) {
        var countQuery = entityManager.createQuery("""
                                                   SELECT COUNT(h) FROM PostTextHighlight h
                                                   WHERE h.user.id = :userId
                                                   """, Long.class)
                                      .setParameter("userId", userId);
        long total = countQuery.getSingleResult();

        var rows = entityManager.createQuery("""
                                             SELECT new dev.vepo.contraponto.highlight.HighlightLibraryRow(
                                                 h.id, p.id, p.title, p.slug, b.slug, u.username,
                                                 h.passage, h.createdAt)
                                             FROM PostTextHighlight h
                                             JOIN h.post p
                                             JOIN p.blog b
                                             JOIN b.owner u
                                             WHERE h.user.id = :userId
                                             ORDER BY h.createdAt DESC
                                             """, HighlightLibraryRow.class)
                                .setParameter("userId", userId)
                                .setFirstResult(query.skip())
                                .setMaxResults(query.maxResults())
                                .getResultList();
        return new Page<>(rows, query.page(), query.limit(), total);
    }

    @Transactional
    public PostTextHighlight save(PostTextHighlight highlight) {
        if (highlight.getId() == null) {
            entityManager.persist(highlight);
            return highlight;
        }
        return entityManager.merge(highlight);
    }
}
