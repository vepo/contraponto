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
public class HighlightNoteRepository {

    private final EntityManager entityManager;

    @Inject
    public HighlightNoteRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public long countByHighlightAndUser(long highlightId, long userId) {
        return entityManager.createQuery("""
                                         SELECT COUNT(n) FROM HighlightNote n
                                         WHERE n.highlight.id = :highlightId AND n.user.id = :userId
                                         """, Long.class)
                            .setParameter("highlightId", highlightId)
                            .setParameter("userId", userId)
                            .getSingleResult();
    }

    public long countPendingPublicByUserAndPost(long userId, long postId) {
        return entityManager.createQuery("""
                                         SELECT COUNT(n) FROM HighlightNote n
                                         JOIN n.highlight h
                                         WHERE n.user.id = :userId AND h.post.id = :postId
                                           AND n.publicNote = true AND n.status = :status
                                         """, Long.class)
                            .setParameter("userId", userId)
                            .setParameter("postId", postId)
                            .setParameter("status", HighlightNoteStatus.PENDING)
                            .getSingleResult();
    }

    public List<HighlightNote> findApprovedPublicForCluster(long postId, String anchorClusterHash) {
        return entityManager.createQuery("""
                                         SELECT n FROM HighlightNote n
                                         JOIN FETCH n.user
                                         JOIN n.highlight h
                                         WHERE h.post.id = :postId AND h.anchorClusterHash = :hash
                                           AND n.publicNote = true AND n.status = :status
                                         ORDER BY n.createdAt ASC
                                         """, HighlightNote.class)
                            .setParameter("postId", postId)
                            .setParameter("hash", anchorClusterHash)
                            .setParameter("status", HighlightNoteStatus.APPROVED)
                            .getResultList();
    }

    public Optional<HighlightNote> findById(long id) {
        return entityManager.createQuery("""
                                         SELECT n FROM HighlightNote n
                                         JOIN FETCH n.highlight h
                                         JOIN FETCH h.post p
                                         JOIN FETCH p.blog b
                                         JOIN FETCH b.owner
                                         JOIN FETCH n.user
                                         WHERE n.id = :id
                                         """, HighlightNote.class)
                            .setParameter("id", id)
                            .getResultStream()
                            .findFirst();
    }

    public Page<NoteManageRow> findPendingPublicForPostAuthor(long authorUserId, PageQuery query) {
        long total = entityManager.createQuery("""
                                               SELECT COUNT(n) FROM HighlightNote n
                                               JOIN n.highlight h
                                               JOIN h.post p
                                               JOIN p.blog b
                                               WHERE b.owner.id = :authorUserId
                                                 AND n.publicNote = true AND n.status = :status
                                               """, Long.class)
                                  .setParameter("authorUserId", authorUserId)
                                  .setParameter("status", HighlightNoteStatus.PENDING)
                                  .getSingleResult();

        var rows = entityManager.createQuery("""
                                             SELECT new dev.vepo.contraponto.highlight.NoteManageRow(
                                                 n.id, p.id, p.title, h.passage, n.body,
                                                 u.name, n.createdAt)
                                             FROM HighlightNote n
                                             JOIN n.highlight h
                                             JOIN h.post p
                                             JOIN p.blog b
                                             JOIN n.user u
                                             WHERE b.owner.id = :authorUserId
                                               AND n.publicNote = true AND n.status = :status
                                             ORDER BY n.createdAt ASC
                                             """, NoteManageRow.class)
                                .setParameter("authorUserId", authorUserId)
                                .setParameter("status", HighlightNoteStatus.PENDING)
                                .setFirstResult(query.skip())
                                .setMaxResults(query.maxResults())
                                .getResultList();
        return new Page<>(rows, query.page(), query.limit(), total);
    }

    @Transactional
    public HighlightNote save(HighlightNote note) {
        if (note.getId() == null) {
            entityManager.persist(note);
            return note;
        }
        return entityManager.merge(note);
    }
}
