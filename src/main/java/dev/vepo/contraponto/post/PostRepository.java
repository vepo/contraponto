package dev.vepo.contraponto.post;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class PostRepository {
    private static final Logger logger = LoggerFactory.getLogger(PostRepository.class);

    private EntityManager entityManager;

    @Inject
    public PostRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public long count() {
        return this.entityManager.createQuery("SELECT COUNT(p) FROM Post p", Long.class)
                                 .getSingleResult();
    }

    public long countByAuthorAndPublished(long authorId, boolean published) {
        return entityManager.createQuery("""
                                         SELECT COUNT(p)
                                         FROM Post p
                                         WHERE p.author.id = :authorId
                                         AND p.published = :published
                                         """, Long.class)
                            .setParameter("authorId", authorId)
                            .setParameter("published", published)
                            .getSingleResult();
    }

    public long countByAuthorUsernameAndPublished(String username) {
        return entityManager.createQuery("""
                                         SELECT COUNT(p)
                                         FROM Post p
                                         WHERE p.author.username = :username AND
                                               p.published = true
                                         """, Long.class)
                            .setParameter("username", username)
                            .getSingleResult();
    }

    private long countPublished() {
        return entityManager.createQuery("""
                                         SELECT COUNT(p)
                                         FROM Post p
                                         WHERE p.published = true
                                         """, Long.class)
                            .getSingleResult();
    }

    private long countFeatured() {
        return entityManager.createQuery("""
                                         SELECT COUNT(p)
                                         FROM Post p
                                         WHERE p.published = true AND
                                               p.featured = true
                                         """, Long.class)
                            .getSingleResult();
    }

    public long countSearchResults(String query) {
        if (Objects.nonNull(query) && !query.isBlank()) {
            return entityManager.createQuery("""
                                             SELECT COUNT(p)
                                             FROM Post p
                                             WHERE p.published = true
                                             AND (LOWER(p.title) LIKE LOWER(:query)
                                                  OR LOWER(p.description) LIKE LOWER(:query)
                                                  OR LOWER(p.content) LIKE LOWER(:query))
                                             """, Long.class)
                                .setParameter("query", "%" + query + "%")
                                .getSingleResult();
        } else {
            return entityManager.createQuery("""
                                             SELECT COUNT(p)
                                             FROM Post p
                                             WHERE p.published = true
                                             """, Long.class)
                                .getSingleResult();
        }
    }

    @Transactional
    public boolean delete(Long id) {
        Post post = entityManager.find(Post.class, id);
        if (post == null) {
            return false;
        }
        entityManager.remove(post);
        logger.info("Deleted post: {}", post.getSlug());
        return true;
    }

    public List<Post> findAll(int offset, int limit) {
        return this.entityManager.createQuery("""
                                              FROM Post
                                              ORDER BY createdAt DESC
                                              """, Post.class)
                                 .setFirstResult(offset)
                                 .setMaxResults(limit)
                                 .getResultList();
    }

    public List<Post> findByAuthor(long authorId) {
        return this.entityManager.createQuery("""
                                              FROM Post
                                              WHERE author.id = :authorId
                                              ORDER BY createdAt DESC
                                              """, Post.class)
                                 .setParameter("authorId", authorId)
                                 .getResultList();
    }

    public List<Post> findByAuthorAndPublished(long authorId, boolean published) {
        return entityManager.createQuery("""
                                         FROM Post p
                                         WHERE author.id = :authorId AND
                                               published = :published

                                         ORDER BY updatedAt DESC
                                         """,
                                         Post.class)
                            .setParameter("authorId", authorId)
                            .setParameter("published", published)
                            .getResultList();
    }

    public Page<Post> findPublished(String username, PageQuery query) {
        return new Page<>(entityManager.createQuery("""
                                                    SELECT p
                                                    FROM Post p
                                                    JOIN FETCH p.author
                                                    WHERE p.author.username = :username AND
                                                          p.published = true
                                                    ORDER BY p.publishedAt DESC
                                                    """, Post.class)
                                       .setParameter("username", username)
                                       .setMaxResults(query.maxResults())
                                       .setFirstResult(query.skip())
                                       .getResultList(),
                          query.page(),
                          query.limit(),
                          countByAuthorUsernameAndPublished(username));
    }

    public Optional<Post> findById(Long id) {
        return Optional.ofNullable(entityManager.find(Post.class, id));
    }

    public Optional<Post> findByUsernameAndSlug(String username, String slug) {
        return this.entityManager.createQuery("""
                                              FROM Post
                                              JOIN FETCH author
                                              WHERE published = TRUE AND
                                                    author.username = :username AND
                                                    slug = :slug
                                              """, Post.class)
                                 .setParameter("username", username)
                                 .setParameter("slug", slug)
                                 .getResultStream()
                                 .findFirst();
    }

    public List<Post> findDrafts() {
        return entityManager.createQuery("FROM Post WHERE published = false", Post.class).getResultList();
    }

    public Page<Post> findPublished(PageQuery query) {
        return new Page<>(entityManager.createQuery("""
                                                    FROM Post
                                                    JOIN FETCH author
                                                    WHERE published = true
                                                    ORDER BY publishedAt DESC
                                                    """, Post.class)
                                       .setMaxResults(query.maxResults())
                                       .setFirstResult(query.skip())
                                       .getResultList(),
                          query.page(),
                          query.limit(),
                          countPublished());
    }

    public Page<Post> findFeatured(PageQuery query) {
        return new Page<>(entityManager.createQuery("""
                                                    FROM Post
                                                    JOIN FETCH author
                                                    WHERE published = true AND
                                                          featured = true
                                                    ORDER BY publishedAt DESC
                                                    """, Post.class)
                                       .setMaxResults(query.maxResults())
                                       .setFirstResult(query.skip())
                                       .getResultList(),
                          query.page(),
                          query.limit(),
                          countFeatured());
    }

    public List<Post> findRecentByAuthorAndPublished(long authorId, boolean published, int limit) {
        return entityManager.createQuery("""
                                         FROM Post p
                                         WHERE p.author.id = :authorId
                                         AND p.published = :published
                                         ORDER BY p.updatedAt DESC
                                         """, Post.class)
                            .setParameter("authorId", authorId)
                            .setParameter("published", published)
                            .setMaxResults(limit)
                            .getResultList();
    }

    @Transactional
    public Post save(Post post) {
        entityManager.persist(post);
        logger.info("Updated post: {}", post.getSlug());
        return post;
    }

    public Page<Post> search(String term, PageQuery query) {
        if (Objects.nonNull(term) && !term.isBlank()) {
            return new Page<>(entityManager.createQuery("""
                                                        FROM Post p
                                                        WHERE p.published = true
                                                        AND (LOWER(p.title) LIKE LOWER(:query)
                                                             OR LOWER(p.description) LIKE LOWER(:query)
                                                             OR LOWER(p.content) LIKE LOWER(:query))
                                                        ORDER BY p.publishedAt DESC
                                                        """, Post.class)
                                           .setParameter("query", "%%%s%%".formatted(term))
                                           .setMaxResults(query.maxResults())
                                           .setFirstResult(query.skip())
                                           .getResultList(),
                              query.page(),
                              query.limit(),
                              countSearch(term));
        } else {
            return new Page<>(entityManager.createQuery("""
                                                        FROM Post p
                                                        WHERE p.published = true
                                                        ORDER BY p.publishedAt DESC
                                                        """, Post.class)
                                           .setMaxResults(query.maxResults())
                                           .setFirstResult(query.skip())
                                           .getResultList(),
                              query.page(),
                              query.limit(),
                              countSearch(term));
        }
    }

    public long countSearch(String term) {
        if (Objects.nonNull(term) && !term.isBlank()) {
            return entityManager.createQuery("""
                                             SELECT COUNT(p)
                                             FROM Post p
                                             WHERE p.published = true
                                             AND (LOWER(p.title) LIKE LOWER(:query)
                                                  OR LOWER(p.description) LIKE LOWER(:query)
                                                  OR LOWER(p.content) LIKE LOWER(:query))
                                             """, Long.class)
                                .setParameter("query", "%%%s%%".formatted(term))
                                .getSingleResult();
        } else {
            return entityManager.createQuery("""
                                             SELECT COUNT(p)
                                             FROM Post p
                                             WHERE p.published = true
                                             """, Long.class)
                                .getSingleResult();
        }
    }

    public boolean slugExists(String slug, Long excludeId) {
        var query = entityManager.createQuery("""
                                              SELECT COUNT(p)
                                              FROM Post p
                                              WHERE p.slug = :slug
                                              AND (:excludeId IS NULL OR p.id != :excludeId)
                                              """, Long.class);
        query.setParameter("slug", slug);
        query.setParameter("excludeId", excludeId);
        return query.getSingleResult() > 0;
    }

    public Object toogleFeatured(Long postId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'toogleFeatured'");
    }
}