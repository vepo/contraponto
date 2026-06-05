package dev.vepo.contraponto.blog;

import java.util.List;
import java.util.Optional;

import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class BlogRepository {

    private static final String PARAM_OWNER_ID = "ownerId";
    private static final String PARAM_BLOG_ID = "blogId";
    private static final String PARAM_SLUG = "slug";
    private static final String PARAM_USERNAME = "username";
    private static final String PARAM_EXCLUDE_BLOG_ID = "excludeBlogId";

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
                            .setParameter(PARAM_OWNER_ID, ownerId)
                            .getSingleResult();
    }

    public boolean existsSlug(long ownerId, String slug, Long excludeBlogId) {
        var cb = entityManager.getCriteriaBuilder();
        var criteria = cb.createQuery(Long.class);
        var root = criteria.from(Blog.class);
        var predicates = cb.and(cb.equal(root.get("owner").get("id"), ownerId), cb.equal(root.get(PARAM_SLUG), slug));
        if (excludeBlogId != null) {
            predicates = cb.and(predicates, cb.notEqual(root.get("id"), excludeBlogId));
        }
        criteria.select(cb.count(root));
        criteria.where(predicates);
        return entityManager.createQuery(criteria).getSingleResult() > 0;
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
                            .setParameter(PARAM_OWNER_ID, ownerId)
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
                            .setParameter(PARAM_USERNAME, username)
                            .setParameter(PARAM_SLUG, slug)
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
                            .setParameter(PARAM_BLOG_ID, blogId)
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
                            .setParameter(PARAM_OWNER_ID, ownerId)
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
                            .setParameter(PARAM_OWNER_ID, ownerId)
                            .getResultStream()
                            .findFirst();
    }

    public Optional<Blog> findMainByOwnerUsername(String username) {
        return entityManager.createQuery("""
                                         FROM Blog b
                                         JOIN FETCH b.owner o
                                         WHERE b.active AND
                                               b.main AND
                                               o.username = :username
                                         """, Blog.class)
                            .setParameter("username", username)
                            .getResultStream()
                            .findFirst();
    }

    public Page<Blog> findPageAllForManagement(PageQuery query) {
        long total = entityManager.createQuery("SELECT COUNT(b) FROM Blog b", Long.class)
                                  .getSingleResult();
        var data = entityManager.createQuery("""
                                             FROM Blog b
                                             JOIN FETCH b.owner
                                             ORDER BY b.owner.username, b.main DESC, b.name
                                             """, Blog.class)
                                .setFirstResult(query.skip())
                                .setMaxResults(query.maxResults())
                                .getResultList();
        return new Page<>(data, query.page(), query.limit(), total);
    }

    public Page<Blog> findPageByOwnerIdForManagement(long ownerId, PageQuery query) {
        long total = entityManager.createQuery("""
                                               SELECT COUNT(b) FROM Blog b
                                               WHERE b.owner.id = :ownerId
                                               """, Long.class)
                                  .setParameter(PARAM_OWNER_ID, ownerId)
                                  .getSingleResult();
        var data = entityManager.createQuery("""
                                             FROM Blog b
                                             JOIN FETCH b.owner
                                             WHERE b.owner.id = :ownerId
                                             ORDER BY b.main DESC, b.name
                                             """, Blog.class)
                                .setParameter(PARAM_OWNER_ID, ownerId)
                                .setFirstResult(query.skip())
                                .setMaxResults(query.maxResults())
                                .getResultList();
        return new Page<>(data, query.page(), query.limit(), total);
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
