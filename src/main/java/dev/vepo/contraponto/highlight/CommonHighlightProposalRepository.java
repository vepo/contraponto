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
public class CommonHighlightProposalRepository {

    private final EntityManager entityManager;

    @Inject
    public CommonHighlightProposalRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<CommonHighlightProposal> findById(long id) {
        return entityManager.createQuery("""
                                         SELECT p FROM CommonHighlightProposal p
                                         JOIN FETCH p.post po
                                         JOIN FETCH po.blog b
                                         JOIN FETCH b.owner
                                         WHERE p.id = :id
                                         """, CommonHighlightProposal.class)
                            .setParameter("id", id)
                            .getResultStream()
                            .findFirst();
    }

    public Optional<CommonHighlightProposal> findByPostAndCluster(long postId, String anchorClusterHash) {
        return entityManager.createQuery("""
                                         SELECT p FROM CommonHighlightProposal p
                                         WHERE p.post.id = :postId AND p.anchorClusterHash = :hash
                                         """, CommonHighlightProposal.class)
                            .setParameter("postId", postId)
                            .setParameter("hash", anchorClusterHash)
                            .getResultStream()
                            .findFirst();
    }

    public List<CommonHighlightProposal> findPendingForPost(long postId) {
        return entityManager.createQuery("""
                                         SELECT p FROM CommonHighlightProposal p
                                         WHERE p.post.id = :postId AND p.status = :status
                                         ORDER BY p.createdAt ASC
                                         """, CommonHighlightProposal.class)
                            .setParameter("postId", postId)
                            .setParameter("status", ProposalStatus.PENDING)
                            .getResultList();
    }

    public Page<ProposalManageRow> findPendingForPostAuthor(long authorUserId, PageQuery query) {
        long total = entityManager.createQuery("""
                                               SELECT COUNT(p) FROM CommonHighlightProposal p
                                               JOIN p.post po
                                               JOIN po.blog b
                                               WHERE b.owner.id = :authorUserId AND p.status = :status
                                               """, Long.class)
                                  .setParameter("authorUserId", authorUserId)
                                  .setParameter("status", ProposalStatus.PENDING)
                                  .getSingleResult();

        var rows = entityManager.createQuery("""
                                             SELECT new dev.vepo.contraponto.highlight.ProposalManageRow(
                                                 p.id, po.id, po.title, p.passage, p.readerCount, p.createdAt)
                                             FROM CommonHighlightProposal p
                                             JOIN p.post po
                                             JOIN po.blog b
                                             WHERE b.owner.id = :authorUserId AND p.status = :status
                                             ORDER BY p.createdAt ASC
                                             """, ProposalManageRow.class)
                                .setParameter("authorUserId", authorUserId)
                                .setParameter("status", ProposalStatus.PENDING)
                                .setFirstResult(query.skip())
                                .setMaxResults(query.maxResults())
                                .getResultList();
        return new Page<>(rows, query.page(), query.limit(), total);
    }

    @Transactional
    public CommonHighlightProposal save(CommonHighlightProposal proposal) {
        if (proposal.getId() == null) {
            entityManager.persist(proposal);
            return proposal;
        }
        return entityManager.merge(proposal);
    }
}
