package dev.vepo.contraponto.blog;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class BlogRepository {

    private final EntityManager entityManager;

    @Inject
    public BlogRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public long countActiveByOwnerId(long ownerId) {
        return entityManager.createQuery("""
                                         SELECT COUNT(b) FROM Blog b
                                         WHERE b.active AND
                                               b.owner.id = :ownerId
                                         """, Long.class)
                            .setParameter("ownerId", ownerId)
                            .getSingleResult();
    }

    public boolean existsSlug(long ownerId, String slug, Long excludeBlogId) {
        var query = new StringBuilder("""
                                      SELECT COUNT(b) FROM Blog b
                                      WHERE b.owner.id = :ownerId AND
                                            b.slug = :slug
                                      """);
        if (excludeBlogId != null) {
            query.append(" AND b.id <> :excludeBlogId");
        }
        var typedQuery = entityManager.createQuery(query.toString(), Long.class)
                                      .setParameter("ownerId", ownerId)
                                      .setParameter("slug", slug);
        if (excludeBlogId != null) {
            typedQuery.setParameter("excludeBlogId", excludeBlogId);
        }
        return typedQuery.getSingleResult() > 0;
    }

    public List<Long> findActiveBlogIdsForGitPoll() {
        return entityManager.createQuery("""
                                         SELECT b.id FROM Blog b
                                         WHERE b.active = true AND
                                               b.gitEnabled = true AND
                                               b.gitRemoteUrl IS NOT NULL AND
                                               TRIM(b.gitRemoteUrl) <> ''
                                         ORDER BY b.id ASC
                                         """,
                                         Long.class)
                            .getResultList();
    }

    public List<Blog> findActiveBlogs(long ownerId) {
        return entityManager.createQuery("""
                                         FROM Blog
                                         WHERE active AND
                                               owner.id = :ownerId
                                         """, Blog.class)
                            .setParameter("ownerId", ownerId)
                            .getResultList();
    }

    public Optional<Blog> findActiveByOwnerUsernameAndSlug(String username, String slug) {
        return entityManager.createQuery("""
                                         FROM Blog b
                                         JOIN FETCH b.owner o
                                         WHERE b.active = true AND
                                               o.username = :username AND
                                               b.slug = :slug
                                         """, Blog.class)
                            .setParameter("username", username)
                            .setParameter("slug", slug)
                            .getResultStream()
                            .findFirst();
    }

    public List<Blog> findAllActiveWithOwner() {
        return entityManager.createQuery("""
                                         FROM Blog b
                                         JOIN FETCH b.owner
                                         WHERE b.active
                                         ORDER BY b.owner.username, b.main DESC, b.name
                                         """, Blog.class)
                            .getResultList();
    }

    public List<Blog> findAllForManagement() {
        return entityManager.createQuery("""
                                         FROM Blog b
                                         JOIN FETCH b.owner
                                         ORDER BY b.owner.username, b.main DESC, b.name
                                         """, Blog.class)
                            .getResultList();
    }

    public Optional<Blog> findById(Long blogId) {
        return entityManager.createQuery("""
                                         FROM Blog b
                                         JOIN FETCH b.owner
                                         WHERE b.id = :blogId
                                         """, Blog.class)
                            .setParameter("blogId", blogId)
                            .getResultStream()
                            .findFirst();
    }

    public List<Blog> findByOwnerIdForManagement(long ownerId) {
        return entityManager.createQuery("""
                                         FROM Blog b
                                         JOIN FETCH b.owner
                                         WHERE b.owner.id = :ownerId
                                         ORDER BY b.main DESC, b.name
                                         """, Blog.class)
                            .setParameter("ownerId", ownerId)
                            .getResultList();
    }

    public Optional<Blog> findMainByOwnerId(long ownerId) {
        return entityManager.createQuery("""
                                         FROM Blog b
                                         JOIN FETCH b.owner
                                         WHERE b.active AND
                                               b.main AND
                                               b.owner.id = :ownerId
                                         """, Blog.class)
                            .setParameter("ownerId", ownerId)
                            .getResultStream()
                            .findFirst();
    }

    @Transactional
    public Blog save(Blog blog) {
        if (blog.getId() == null) {
            entityManager.persist(blog);
            return blog;
        }
        return entityManager.merge(blog);
    }

}
