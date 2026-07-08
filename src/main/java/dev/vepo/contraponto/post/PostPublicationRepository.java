package dev.vepo.contraponto.post;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class PostPublicationRepository {

    private final EntityManager entityManager;

    @Inject
    public PostPublicationRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<PostPublication> findById(long id) {
        return Optional.ofNullable(entityManager.find(PostPublication.class, id));
    }

    public List<PostPublication> findByPostIdOrderByVersionDesc(long postId) {
        return entityManager.createQuery("""
                                         SELECT p FROM PostPublication p
                                         LEFT JOIN FETCH p.tags
                                         LEFT JOIN FETCH p.cover
                                         WHERE p.post.id = :postId
                                         ORDER BY p.version DESC
                                         """, PostPublication.class)
                            .setParameter("postId", postId)
                            .getResultList();
    }

    public Optional<PostPublication> findLatestByPostId(long postId) {
        return findByPostIdOrderByVersionDesc(postId).stream().findFirst();
    }

    public Optional<LocalDateTime> findMaxPublishedAtByBlogId(long blogId) {
        return optionalMaxPublishedAt("""
                                      SELECT MAX(pub.publishedAt) FROM PostPublication pub
                                      JOIN pub.post p
                                      WHERE p.published = true AND p.blog.id = :blogId
                                      """, "blogId", blogId);
    }

    public Optional<LocalDateTime> findMaxPublishedAtBySerieId(long serieId) {
        return optionalMaxPublishedAt("""
                                      SELECT MAX(pub.publishedAt) FROM PostPublication pub
                                      JOIN pub.post p
                                      WHERE p.published = true AND p.serie.id = :serieId
                                      """, "serieId", serieId);
    }

    public Optional<LocalDateTime> findMaxPublishedAtByTagSlug(String tagSlug) {
        return optionalMaxPublishedAt("""
                                      SELECT MAX(pub.publishedAt) FROM PostPublication pub
                                      JOIN pub.post p
                                      JOIN p.blog b
                                      JOIN p.tags t
                                      WHERE p.published = true AND b.active = true AND t.slug = :tagSlug
                                      """, "tagSlug", tagSlug);
    }

    public Optional<LocalDateTime> findMaxPublishedAtSiteWide() {
        return optionalMaxPublishedAt("""
                                      SELECT MAX(pub.publishedAt) FROM PostPublication pub
                                      JOIN pub.post p
                                      JOIN p.blog b
                                      WHERE p.published = true AND b.active = true
                                      """, null, null);
    }

    public Optional<Integer> findMaxVersion(long postId) {
        Integer max = entityManager.createQuery("""
                                                SELECT MAX(p.version) FROM PostPublication p
                                                WHERE p.post.id = :postId
                                                """, Integer.class)
                                   .setParameter("postId", postId)
                                   .getSingleResult();
        return Optional.ofNullable(max);
    }

    public void flush() {
        entityManager.flush();
    }

    private Optional<LocalDateTime> optionalMaxPublishedAt(String jpql, String paramName, Object paramValue) {
        var query = entityManager.createQuery(jpql, LocalDateTime.class);
        if (paramName != null) {
            query.setParameter(paramName, paramValue);
        }
        try {
            return Optional.ofNullable(query.getSingleResult());
        } catch (NoResultException _) {
            return Optional.empty();
        }
    }

    @Transactional
    public PostPublication save(PostPublication publication) {
        entityManager.persist(publication);
        return publication;
    }

    @Transactional
    public boolean updateRenderedHtmlIfNull(long publicationId, String renderedHtml) {
        int updated = entityManager.createQuery("""
                                                UPDATE PostPublication p
                                                SET p.renderedHtml = :renderedHtml
                                                WHERE p.id = :id AND p.renderedHtml IS NULL
                                                """)
                                   .setParameter("renderedHtml", renderedHtml)
                                   .setParameter("id", publicationId)
                                   .executeUpdate();
        return updated > 0;
    }
}
