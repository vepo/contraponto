package dev.vepo.contraponto.postresponse;

import java.util.List;
import java.util.Optional;

import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class PostResponseRepository {

    private final EntityManager entityManager;

    @Inject
    public PostResponseRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<PostResponse> findApprovedForSourcePost(long sourcePostId) {
        return entityManager.createQuery("""
                                         SELECT r FROM PostResponse r
                                         JOIN FETCH r.responsePost rp
                                         JOIN FETCH rp.blog b
                                         JOIN FETCH b.owner
                                         JOIN FETCH r.responder
                                         WHERE r.sourcePost.id = :sourcePostId
                                           AND r.linkBackStatus = :status
                                         ORDER BY r.createdAt DESC
                                         """, PostResponse.class)
                            .setParameter("sourcePostId", sourcePostId)
                            .setParameter("status", PostResponseLinkBackStatus.APPROVED)
                            .getResultList();
    }

    public Optional<PostResponse> findById(long id) {
        return entityManager.createQuery("""
                                         SELECT r FROM PostResponse r
                                         JOIN FETCH r.sourcePost sp
                                         JOIN FETCH sp.blog sb
                                         JOIN FETCH sb.owner
                                         JOIN FETCH r.responsePost rp
                                         JOIN FETCH rp.blog rb
                                         JOIN FETCH rb.owner
                                         JOIN FETCH r.responder
                                         WHERE r.id = :id
                                         """, PostResponse.class)
                            .setParameter("id", id)
                            .getResultStream()
                            .findFirst();
    }

    public Optional<PostResponse> findByResponsePostId(long responsePostId) {
        return entityManager.createQuery("""
                                         SELECT r FROM PostResponse r
                                         JOIN FETCH r.sourcePost sp
                                         JOIN FETCH sp.blog sb
                                         JOIN FETCH sb.owner
                                         JOIN FETCH r.responsePost
                                         WHERE r.responsePost.id = :responsePostId
                                         """, PostResponse.class)
                            .setParameter("responsePostId", responsePostId)
                            .getResultStream()
                            .findFirst();
    }

    public Page<PostResponseManageRow> findForPostAuthor(long authorUserId, PageQuery query) {
        long total = entityManager.createQuery("""
                                               SELECT COUNT(r) FROM PostResponse r
                                               JOIN r.sourcePost sp
                                               JOIN sp.blog b
                                               WHERE b.owner.id = :authorUserId
                                                 AND r.linkBackStatus IN :statuses
                                               """, Long.class)
                                  .setParameter("authorUserId", authorUserId)
                                  .setParameter("statuses", List.of(PostResponseLinkBackStatus.PENDING,
                                                                    PostResponseLinkBackStatus.APPROVED))
                                  .getSingleResult();

        var rows = entityManager.createQuery("""
                                             SELECT new dev.vepo.contraponto.postresponse.PostResponseManageRow(
                                                 r.id, sp.id, sp.title, rp.id, rp.title,
                                                 resp.name, rb.name, r.linkBackStatus, r.createdAt)
                                             FROM PostResponse r
                                             JOIN r.sourcePost sp
                                             JOIN sp.blog sb
                                             JOIN r.responsePost rp
                                             JOIN rp.blog rb
                                             JOIN r.responder resp
                                             WHERE sb.owner.id = :authorUserId
                                               AND r.linkBackStatus IN :statuses
                                             ORDER BY r.createdAt DESC
                                             """, PostResponseManageRow.class)
                                .setParameter("authorUserId", authorUserId)
                                .setParameter("statuses", List.of(PostResponseLinkBackStatus.PENDING,
                                                                  PostResponseLinkBackStatus.APPROVED))
                                .setFirstResult(query.skip())
                                .setMaxResults(query.maxResults())
                                .getResultList();
        return new Page<>(rows, query.page(), query.limit(), total);
    }

    @Transactional
    public PostResponse save(PostResponse response) {
        if (response.getId() == null) {
            entityManager.persist(response);
            return response;
        }
        return entityManager.merge(response);
    }
}
