package dev.vepo.contraponto.post;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class PostRepository {
    private static final Logger logger = LoggerFactory.getLogger(PostRepository.class);

    private EntityManager entityManager;

    @Inject
    public PostRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<Post> findBySlug(String slug) {
        return this.entityManager.createQuery("""
                                              FROM Post
                                              WHERE published = TRUE AND
                                                    slug = :slug
                                              """, Post.class)
                                 .setParameter("slug", slug)
                                 .getResultStream()
                                 .findFirst();
    }

    public List<Post> findNewest(int limit) {
        return this.entityManager.createQuery("""
                                              FROM Post
                                              WHERE published = TRUE
                                              ORDER BY publishedAt DESC
                                              """, Post.class)
                                 .setMaxResults(limit)
                                 .getResultList();
    }
}
