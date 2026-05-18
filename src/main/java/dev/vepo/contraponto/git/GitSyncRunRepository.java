package dev.vepo.contraponto.git;

import java.util.List;
import java.util.Optional;

import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class GitSyncRunRepository {

    private static final int RETENTION_PER_BLOG = 100;

    private final EntityManager entityManager;

    @Inject
    public GitSyncRunRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public GitSyncRun create(GitSyncRun run) {
        entityManager.persist(run);
        return run;
    }

    @Transactional
    public GitSyncRunEntry createEntry(GitSyncRunEntry entry) {
        entityManager.persist(entry);
        return entry;
    }

    public Optional<GitSyncRun> findById(long runId) {
        return entityManager.createQuery("""
                                         SELECT r FROM GitSyncRun r
                                         JOIN FETCH r.blog b
                                         LEFT JOIN FETCH b.owner
                                         LEFT JOIN FETCH r.post
                                         WHERE r.id = :runId
                                         """, GitSyncRun.class)
                            .setParameter("runId", runId)
                            .getResultStream()
                            .findFirst();
    }

    public Optional<GitSyncRun> findByIdAndBlogId(long runId, long blogId) {
        return entityManager.createQuery("""
                                         SELECT r FROM GitSyncRun r
                                         JOIN FETCH r.blog
                                         LEFT JOIN FETCH r.post
                                         WHERE r.id = :runId AND r.blog.id = :blogId
                                         """, GitSyncRun.class)
                            .setParameter("runId", runId)
                            .setParameter("blogId", blogId)
                            .getResultStream()
                            .findFirst();
    }

    public Page<GitSyncRun> findPageByBlog(long blogId, PageQuery query) {
        long total = entityManager.createQuery("""
                                               SELECT COUNT(r)
                                               FROM GitSyncRun r
                                               WHERE r.blog.id = :blogId
                                               """, Long.class)
                                  .setParameter("blogId", blogId)
                                  .getSingleResult();

        List<GitSyncRun> data = entityManager.createQuery("""
                                                          SELECT r FROM GitSyncRun r
                                                          WHERE r.blog.id = :blogId
                                                          ORDER BY r.startedAt DESC
                                                          """, GitSyncRun.class)
                                             .setParameter("blogId", blogId)
                                             .setFirstResult(query.skip())
                                             .setMaxResults(query.limit())
                                             .getResultList();

        return new Page<>(data, query.page(), query.limit(), total);
    }

    public List<GitSyncRunEntry> listEntries(long runId) {
        return entityManager.createQuery("""
                                         SELECT e FROM GitSyncRunEntry e
                                         LEFT JOIN FETCH e.post
                                         WHERE e.run.id = :runId
                                         ORDER BY e.sequence ASC
                                         """, GitSyncRunEntry.class)
                            .setParameter("runId", runId)
                            .getResultList();
    }

    public int nextSequence(long runId) {
        Number max = entityManager.createQuery("""
                                               SELECT COALESCE(MAX(e.sequence), 0)
                                               FROM GitSyncRunEntry e
                                               WHERE e.run.id = :runId
                                               """, Number.class)
                                  .setParameter("runId", runId)
                                  .getSingleResult();
        return max.intValue() + 1;
    }

    @Transactional
    public void pruneOldRuns(long blogId) {
        entityManager.createNativeQuery("""
                                        DELETE FROM tb_git_sync_runs
                                        WHERE blog_id = :blogId
                                          AND id NOT IN (
                                              SELECT id FROM (
                                                  SELECT id FROM tb_git_sync_runs
                                                  WHERE blog_id = :blogId
                                                  ORDER BY started_at DESC
                                                  LIMIT :keep
                                              ) kept
                                          )
                                        """)
                     .setParameter("blogId", blogId)
                     .setParameter("keep", RETENTION_PER_BLOG)
                     .executeUpdate();
    }

    @Transactional
    public void update(GitSyncRun run) {
        entityManager.merge(run);
    }
}
