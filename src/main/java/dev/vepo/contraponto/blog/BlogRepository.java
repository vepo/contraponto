package dev.vepo.contraponto.blog;

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
