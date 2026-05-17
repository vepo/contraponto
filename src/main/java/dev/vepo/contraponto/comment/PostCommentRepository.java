package dev.vepo.contraponto.comment;

import java.util.List;
import java.util.Optional;

import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class PostCommentRepository {

    private final EntityManager entityManager;

    @Inject
    public PostCommentRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public long countApprovedReplies(long rootId) {
        return entityManager.createQuery("""
                                         SELECT COUNT(c)
                                         FROM PostComment c
                                         WHERE c.root.id = :rootId AND c.status = :status
                                         """, Long.class)
                            .setParameter("rootId", rootId)
                            .setParameter("status", CommentStatus.APPROVED)
                            .getSingleResult();
    }

    public Optional<PostComment> findById(long commentId) {
        return entityManager.createQuery("""
                                         SELECT c FROM PostComment c
                                         JOIN FETCH c.post p
                                         JOIN FETCH p.blog b
                                         JOIN FETCH b.owner
                                         JOIN FETCH c.author
                                         LEFT JOIN FETCH c.parent
                                         LEFT JOIN FETCH c.root
                                         WHERE c.id = :id
                                         """, PostComment.class)
                            .setParameter("id", commentId)
                            .getResultStream()
                            .findFirst();
    }

    public List<PostComment> findPendingForPost(long postId) {
        return entityManager.createQuery("""
                                         SELECT c FROM PostComment c
                                         JOIN FETCH c.author
                                         LEFT JOIN FETCH c.parent
                                         WHERE c.post.id = :postId AND c.status = :status
                                         ORDER BY c.createdAt ASC
                                         """, PostComment.class)
                            .setParameter("postId", postId)
                            .setParameter("status", CommentStatus.PENDING)
                            .getResultList();
    }

    public List<PostComment> findPendingForPostAuthor(long authorUserId) {
        return entityManager.createQuery("""
                                         SELECT c FROM PostComment c
                                         JOIN FETCH c.post p
                                         JOIN FETCH p.blog b
                                         JOIN FETCH b.owner
                                         JOIN FETCH c.author
                                         LEFT JOIN FETCH c.parent
                                         WHERE b.owner.id = :authorUserId AND c.status = :status
                                         ORDER BY c.createdAt ASC
                                         """, PostComment.class)
                            .setParameter("authorUserId", authorUserId)
                            .setParameter("status", CommentStatus.PENDING)
                            .getResultList();
    }

    public Page<PostComment> findPendingPageForPostAuthor(long authorUserId, PageQuery query) {
        long total = entityManager.createQuery("""
                                               SELECT COUNT(c) FROM PostComment c
                                               JOIN c.post p
                                               JOIN p.blog b
                                               WHERE b.owner.id = :authorUserId AND c.status = :status
                                               """, Long.class)
                                  .setParameter("authorUserId", authorUserId)
                                  .setParameter("status", CommentStatus.PENDING)
                                  .getSingleResult();
        var data = entityManager.createQuery("""
                                             SELECT c FROM PostComment c
                                             JOIN FETCH c.post p
                                             JOIN FETCH p.blog b
                                             JOIN FETCH b.owner
                                             JOIN FETCH c.author
                                             LEFT JOIN FETCH c.parent
                                             WHERE b.owner.id = :authorUserId AND c.status = :status
                                             ORDER BY c.createdAt ASC
                                             """, PostComment.class)
                                .setParameter("authorUserId", authorUserId)
                                .setParameter("status", CommentStatus.PENDING)
                                .setFirstResult(query.skip())
                                .setMaxResults(query.maxResults())
                                .getResultList();
        return new Page<>(data, query.page(), query.limit(), total);
    }

    public List<PostComment> findRepliesByRootId(long rootId) {
        return entityManager.createQuery("""
                                         SELECT c FROM PostComment c
                                         JOIN FETCH c.author
                                         JOIN FETCH c.parent
                                         WHERE c.root.id = :rootId
                                         ORDER BY c.createdAt ASC
                                         """, PostComment.class)
                            .setParameter("rootId", rootId)
                            .getResultList();
    }

    public List<PostComment> findRootComments(long postId) {
        return entityManager.createQuery("""
                                         SELECT c FROM PostComment c
                                         JOIN FETCH c.author
                                         WHERE c.post.id = :postId AND c.parent IS NULL
                                         ORDER BY c.createdAt ASC
                                         """, PostComment.class)
                            .setParameter("postId", postId)
                            .getResultList();
    }

    @Transactional
    public PostComment save(PostComment comment) {
        if (comment.getId() == null) {
            entityManager.persist(comment);
            return comment;
        }
        return entityManager.merge(comment);
    }
}
