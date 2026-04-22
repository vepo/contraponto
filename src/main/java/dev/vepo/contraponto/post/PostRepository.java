package dev.vepo.contraponto.post;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.image.Image;
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

    public Optional<Post> findById(Long id) {
        return Optional.ofNullable(entityManager.find(Post.class, id));
    }

    public Optional<Post> findBySlugForEdit(String slug) {
        return this.entityManager.createQuery("""
                                              FROM Post
                                              WHERE slug = :slug
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

    public List<Post> findAll(int offset, int limit) {
        return this.entityManager.createQuery("""
                                              FROM Post
                                              ORDER BY createdAt DESC
                                              """, Post.class)
                                 .setFirstResult(offset)
                                 .setMaxResults(limit)
                                 .getResultList();
    }

    public long count() {
        return this.entityManager.createQuery("SELECT COUNT(p) FROM Post p", Long.class)
                                 .getSingleResult();
    }

    public List<Post> findByAuthor(String author) {
        return this.entityManager.createQuery("""
                                              FROM Post
                                              WHERE author = :author
                                              ORDER BY createdAt DESC
                                              """, Post.class)
                                 .setParameter("author", author)
                                 .getResultList();
    }

    @Transactional
    public Post save(Post post) {
        entityManager.persist(post);
        logger.info("Updated post: {}", post.getSlug());
        return post;
    }

    @Transactional
    public Post update(Long id, PostRequest request, Image cover) {
        Post post = entityManager.find(Post.class, id);
        if (post == null) {
            return null;
        }

        post.setSlug(request.slug());
        post.setTitle(request.title());
        post.setDescription(request.description());
        post.setContent(request.content());
        post.setCover(cover);

        // Handle publishing status change
        if (request.published() && !post.isPublished()) {
            post.setPublished(true);
            post.setPublishedAt(LocalDateTime.now());
        } else if (!request.published() && post.isPublished()) {
            post.setPublished(false);
            post.setPublishedAt(null);
        }

        entityManager.merge(post);
        logger.info("Updated post: {}", post.getSlug());
        return post;
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
}