package dev.vepo.contraponto.highlight;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class OfficialHighlightRepository {

    private final EntityManager entityManager;

    @Inject
    public OfficialHighlightRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<OfficialHighlight> findByPostAndCluster(long postId, String anchorClusterHash) {
        return entityManager.createQuery("""
                                         SELECT o FROM OfficialHighlight o
                                         WHERE o.post.id = :postId AND o.anchorClusterHash = :hash
                                         """, OfficialHighlight.class)
                            .setParameter("postId", postId)
                            .setParameter("hash", anchorClusterHash)
                            .getResultStream()
                            .findFirst();
    }

    public List<OfficialHighlight> findVisibleForPost(long postId) {
        return entityManager.createQuery("""
                                         SELECT o FROM OfficialHighlight o
                                         WHERE o.post.id = :postId AND o.needsReview = false
                                         ORDER BY o.approvedAt ASC
                                         """, OfficialHighlight.class)
                            .setParameter("postId", postId)
                            .getResultList();
    }

    @Transactional
    public OfficialHighlight save(OfficialHighlight official) {
        if (official.getId() == null) {
            entityManager.persist(official);
            return official;
        }
        return entityManager.merge(official);
    }
}
