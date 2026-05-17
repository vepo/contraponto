package dev.vepo.contraponto.notification;

import java.util.List;
import java.util.Optional;

import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class BlogAudienceRepository {

    private final EntityManager entityManager;

    @Inject
    public BlogAudienceRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public void delete(BlogAudience audience) {
        entityManager.remove(entityManager.contains(audience) ? audience : entityManager.merge(audience));
    }

    public Optional<BlogAudience> findByUserAndBlog(long userId, long blogId) {
        return entityManager.createQuery("""
                                         FROM BlogAudience a
                                         WHERE a.user.id = :userId AND a.blog.id = :blogId
                                         """, BlogAudience.class)
                            .setParameter("userId", userId)
                            .setParameter("blogId", blogId)
                            .getResultStream()
                            .findFirst();
    }

    public List<BlogAudience> findByUserId(long userId) {
        return entityManager.createQuery("""
                                         FROM BlogAudience a
                                         JOIN FETCH a.blog b
                                         JOIN FETCH b.owner
                                         WHERE a.user.id = :userId
                                         ORDER BY b.name ASC
                                         """, BlogAudience.class)
                            .setParameter("userId", userId)
                            .getResultList();
    }

    public List<BlogAudience> findEmailSubscribersByBlogId(long blogId) {
        return entityManager.createQuery("""
                                         FROM BlogAudience a
                                         WHERE a.blog.id = :blogId AND a.emailSubscribed = TRUE
                                         """, BlogAudience.class)
                            .setParameter("blogId", blogId)
                            .getResultList();
    }

    public List<BlogAudience> findFollowersByBlogId(long blogId) {
        return entityManager.createQuery("""
                                         FROM BlogAudience a
                                         WHERE a.blog.id = :blogId AND a.followed = TRUE
                                         """, BlogAudience.class)
                            .setParameter("blogId", blogId)
                            .getResultList();
    }

    public Page<BlogAudience> findPageByUserId(long userId, PageQuery query) {
        long total = entityManager.createQuery("""
                                               SELECT COUNT(a) FROM BlogAudience a
                                               WHERE a.user.id = :userId
                                               """, Long.class)
                                  .setParameter("userId", userId)
                                  .getSingleResult();
        var data = entityManager.createQuery("""
                                             FROM BlogAudience a
                                             JOIN FETCH a.blog b
                                             JOIN FETCH b.owner
                                             WHERE a.user.id = :userId
                                             ORDER BY b.name ASC
                                             """, BlogAudience.class)
                                .setParameter("userId", userId)
                                .setFirstResult(query.skip())
                                .setMaxResults(query.maxResults())
                                .getResultList();
        return new Page<>(data, query.page(), query.limit(), total);
    }

    @Transactional
    public BlogAudience save(BlogAudience audience) {
        if (audience.getId() == null) {
            entityManager.persist(audience);
        } else {
            entityManager.merge(audience);
        }
        return audience;
    }
}
