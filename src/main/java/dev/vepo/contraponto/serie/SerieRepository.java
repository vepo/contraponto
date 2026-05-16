package dev.vepo.contraponto.serie;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class SerieRepository {

    private final EntityManager entityManager;

    @Inject
    public SerieRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<Serie> findByBlogIdAndSlug(long blogId, String slug) {
        return entityManager.createQuery("""
                                         SELECT s FROM Serie s
                                         WHERE s.blog.id = :blogId AND s.slug = :slug
                                         """, Serie.class)
                            .setParameter("blogId", blogId)
                            .setParameter("slug", slug)
                            .getResultStream()
                            .findFirst();
    }

    public Optional<Serie> findMainBlogSerie(String username, String serieSlug) {
        return entityManager.createQuery("""
                                         SELECT s FROM Serie s
                                         JOIN FETCH s.blog b
                                         JOIN FETCH b.owner o
                                         WHERE o.username = :username AND
                                               b.main = TRUE AND
                                               s.slug = :slug
                                         """, Serie.class)
                            .setParameter("username", username)
                            .setParameter("slug", serieSlug)
                            .getResultStream()
                            .findFirst();
    }

    public Optional<Serie> findSecondaryBlogSerie(String username, String blogSlug, String serieSlug) {
        return entityManager.createQuery("""
                                         SELECT s FROM Serie s
                                         JOIN FETCH s.blog b
                                         JOIN FETCH b.owner o
                                         WHERE o.username = :username AND
                                               b.main = FALSE AND
                                               b.slug = :blogSlug AND
                                               s.slug = :slug
                                         """, Serie.class)
                            .setParameter("username", username)
                            .setParameter("blogSlug", blogSlug)
                            .setParameter("slug", serieSlug)
                            .getResultStream()
                            .findFirst();
    }

    public Serie persist(Serie serie) {
        entityManager.persist(serie);
        return serie;
    }
}
