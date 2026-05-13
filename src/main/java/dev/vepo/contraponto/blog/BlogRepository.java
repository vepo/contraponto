package dev.vepo.contraponto.blog;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class BlogRepository {

    private final EntityManager entityManager;

    @Inject
    public BlogRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
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

    public Optional<Blog> findById(Long blogId) {
        return entityManager.createQuery("""
                                         FROM Blog
                                         WHERE id = :blogId
                                         """, Blog.class)
                            .setParameter("blogId", blogId)
                            .getResultStream()
                            .findFirst();
    }

}
