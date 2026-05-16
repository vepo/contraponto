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
@SuppressWarnings("java:S1192")
public class PostRepository {
    private static final Logger logger = LoggerFactory.getLogger(PostRepository.class);

    private EntityManager entityManager;
    private PostPublicationRepository publicationRepository;

    @Inject
    public PostRepository(EntityManager entityManager, PostPublicationRepository publicationRepository) {
        this.entityManager = entityManager;
        this.publicationRepository = publicationRepository;
    }

    private void attachLatestPublication(Post post) {
        if (post != null && post.isPublished()) {
            publicationRepository.findLatestByPostId(post.getId()).ifPresent(post::setLivePublication);
        }
    }

    private List<Post> attachLatestPublications(List<Post> posts) {
        posts.forEach(this::attachLatestPublication);
        return posts;
    }

    public long count() {
        return this.entityManager.createQuery("SELECT COUNT(p) FROM Post p", Long.class)
                                 .getSingleResult();
    }

    public long countByAuthorAndPublished(long authorId, boolean published) {
        return entityManager.createQuery("""
                                         SELECT COUNT(p)
                                         FROM Post p
                                         WHERE p.blog.owner.id = :authorId
                                         AND p.published = :published
                                         """, Long.class)
                            .setParameter("authorId", authorId)
                            .setParameter("published", published)
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

    private long countPublished() {
        return entityManager.createQuery("""
                                         SELECT COUNT(p)
                                         FROM Post p
                                         WHERE p.published = true
                                         """, Long.class)
                            .getSingleResult();
    }

    public long countPublishedByAuthor(long authorId) {
        return entityManager.createQuery("""
                                         SELECT COUNT(p)
                                         FROM Post p
                                         WHERE p.blog.owner.id = :authorId AND
                                               p.published = true
                                         """, Long.class)
                            .setParameter("authorId", authorId)
                            .getSingleResult();
    }

    private long countPublishedByTagSlug(String tagSlug) {
        return entityManager.createQuery("""
                                         SELECT COUNT(DISTINCT p.id)
                                         FROM Post p
                                         JOIN p.tags t
                                         WHERE t.slug = :slug AND p.published = true
                                         """, Long.class)
                            .setParameter("slug", tagSlug)
                            .getSingleResult();
    }

    public long countSearch(String term) {
        return entityManager.createQuery("""
                                         SELECT COUNT(p)
                                         FROM Post p
                                         JOIN p.livePublication lp
                                         WHERE p.published = true
                                         AND (LOWER(lp.title) LIKE LOWER(:query)
                                             OR LOWER(lp.description) LIKE LOWER(:query)
                                             OR LOWER(lp.content) LIKE LOWER(:query))
                                         """, Long.class)
                            .setParameter("query", "%%%s%%".formatted(term))
                            .getSingleResult();
    }

    public long countSearchResults(String query) {
        if (Objects.nonNull(query) && !query.isBlank()) {
            return entityManager.createQuery("""
                                             SELECT COUNT(p)
                                             FROM Post p
                                             JOIN p.livePublication lp
                                             WHERE p.published = true
                                             AND (LOWER(lp.title) LIKE LOWER(:query)
                                                  OR LOWER(lp.description) LIKE LOWER(:query)
                                                  OR LOWER(lp.content) LIKE LOWER(:query))
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

    public Optional<Post> findBlogPost(String username, String blogSlug, String slug) {
        return this.entityManager.createQuery("""
                                              SELECT DISTINCT p FROM Post p
                                              JOIN FETCH p.blog b
                                              JOIN FETCH b.owner o
                                              LEFT JOIN FETCH p.serie
                                              LEFT JOIN FETCH p.livePublication lp
                                              LEFT JOIN FETCH lp.tags
                                              WHERE p.published = TRUE AND
                                                    b.main = FALSE AND
                                                    b.slug = :blogSlug AND
                                                    o.username = :username AND
                                                    p.slug = :slug
                                              """, Post.class)
                                 .setParameter("username", username)
                                 .setParameter("blogSlug", blogSlug)
                                 .setParameter("slug", slug)
                                 .getResultStream()
                                 .findFirst()
                                 .map(post -> {
                                     attachLatestPublication(post);
                                     return post;
                                 });
    }

    public List<Post> findByAuthorAndPublished(long authorId, boolean published) {
        return entityManager.createQuery("""
                                         SELECT DISTINCT p FROM Post p
                                         LEFT JOIN FETCH p.tags
                                         WHERE p.blog.owner.id = :authorId AND
                                               p.published = :published
                                         ORDER BY p.updatedAt DESC
                                         """,
                                         Post.class)
                            .setParameter("authorId", authorId)
                            .setParameter("published", published)
                            .getResultList();
    }

    public Optional<Post> findByBlogIdAndSlugWithTags(Long blogId, String slug) {
        return entityManager.createQuery("""
                                         SELECT DISTINCT p FROM Post p
                                         JOIN FETCH p.blog b
                                         LEFT JOIN FETCH p.tags
                                         LEFT JOIN FETCH p.serie
                                         WHERE b.id = :blogId AND
                                               p.slug = :slug
                                         """, Post.class)
                            .setParameter("blogId", blogId)
                            .setParameter("slug", slug)
                            .getResultStream()
                            .findFirst();
    }

    public Optional<Post> findById(Long id) {
        return Optional.ofNullable(entityManager.find(Post.class, id));
    }

    public Optional<Post> findByIdWithTags(Long id) {
        return entityManager.createQuery("""
                                         SELECT DISTINCT p FROM Post p
                                         LEFT JOIN FETCH p.livePublication lp
                                         LEFT JOIN FETCH p.tags
                                         LEFT JOIN FETCH p.serie
                                         WHERE p.id = :id
                                         """, Post.class)
                            .setParameter("id", id)
                            .getResultStream()
                            .findFirst()
                            .map(post -> {
                                attachLatestPublication(post);
                                return post;
                            });
    }

    public List<Post> findDrafts() {
        return entityManager.createQuery("""
                                         SELECT DISTINCT p FROM Post p
                                         LEFT JOIN FETCH p.tags
                                         WHERE p.published = false
                                         """, Post.class)
                            .getResultList();
    }

    public Page<Post> findFeatured(PageQuery query) {
        return new Page<>(attachLatestPublications(entityManager.createQuery("""
                                                                             SELECT DISTINCT p FROM Post p
                                                                             JOIN FETCH p.blog b
                                                                             JOIN FETCH b.owner o
                                                                             LEFT JOIN FETCH p.tags
                                                                             WHERE p.published = true AND
                                                                                   p.featured = true
                                                                             ORDER BY p.publishedAt DESC
                                                                             """, Post.class)
                                                                .setMaxResults(query.maxResults())
                                                                .setFirstResult(query.skip())
                                                                .getResultList()),
                          query.page(),
                          query.limit(),
                          countFeatured());
    }

    public Optional<Post> findMainBlogPost(String username, String slug) {
        return this.entityManager.createQuery("""
                                              SELECT DISTINCT p FROM Post p
                                              JOIN FETCH p.blog b
                                              JOIN FETCH b.owner o
                                              LEFT JOIN FETCH p.serie
                                              LEFT JOIN FETCH p.livePublication lp
                                              LEFT JOIN FETCH lp.tags
                                              WHERE p.published = TRUE AND
                                                    b.main = TRUE AND
                                                    o.username = :username AND
                                                    p.slug = :slug
                                              """, Post.class)
                                 .setParameter("username", username)
                                 .setParameter("slug", slug)
                                 .getResultStream()
                                 .findFirst()
                                 .map(post -> {
                                     attachLatestPublication(post);
                                     return post;
                                 });
    }

    public Page<Post> findPublished(PageQuery query) {
        return new Page<>(attachLatestPublications(entityManager.createQuery("""
                                                                             SELECT DISTINCT p FROM Post p
                                                                             JOIN FETCH p.blog b
                                                                             JOIN FETCH b.owner o
                                                                             LEFT JOIN FETCH p.tags
                                                                             WHERE p.published = true
                                                                             ORDER BY p.publishedAt DESC
                                                                             """, Post.class)
                                                                .setMaxResults(query.maxResults())
                                                                .setFirstResult(query.skip())
                                                                .getResultList()),
                          query.page(),
                          query.limit(),
                          countPublished());
    }

    public Page<Post> findPublishedByAuthor(long authorId, PageQuery query) {
        return new Page<>(attachLatestPublications(entityManager.createQuery("""
                                                                             SELECT DISTINCT p FROM Post p
                                                                             JOIN FETCH p.blog b
                                                                             JOIN FETCH b.owner o
                                                                             LEFT JOIN FETCH p.tags
                                                                             WHERE b.owner.id = :authorId AND
                                                                                   p.published = true
                                                                             ORDER BY p.publishedAt DESC
                                                                             """, Post.class)
                                                                .setParameter("authorId", authorId)
                                                                .setMaxResults(query.maxResults())
                                                                .setFirstResult(query.skip())
                                                                .getResultList()),
                          query.page(),
                          query.limit(),
                          countPublishedByAuthor(authorId));
    }

    public List<Post> findPublishedBySerieOrdered(long serieId) {
        return attachLatestPublications(entityManager.createQuery("""
                                                                  SELECT DISTINCT p FROM Post p
                                                                  JOIN FETCH p.blog b
                                                                  JOIN FETCH b.owner o
                                                                  LEFT JOIN FETCH p.tags
                                                                  WHERE p.serie.id = :serieId AND p.published = true
                                                                  ORDER BY p.publishedAt ASC NULLS LAST, p.id ASC
                                                                  """, Post.class)
                                                     .setParameter("serieId", serieId)
                                                     .getResultList());
    }

    public Page<Post> findPublishedByTagSlug(String tagSlug, PageQuery query) {
        return new Page<>(attachLatestPublications(entityManager.createQuery("""
                                                                             SELECT DISTINCT p FROM Post p
                                                                             JOIN FETCH p.blog b
                                                                             JOIN FETCH b.owner o
                                                                             LEFT JOIN FETCH p.tags
                                                                             WHERE p.published = true AND
                                                                                   EXISTS (SELECT 1 FROM Post p2 JOIN p2.tags t
                                                                                           WHERE p2.id = p.id AND t.slug = :slug)
                                                                             ORDER BY p.publishedAt DESC
                                                                             """, Post.class)
                                                                .setParameter("slug", tagSlug)
                                                                .setMaxResults(query.maxResults())
                                                                .setFirstResult(query.skip())
                                                                .getResultList()),
                          query.page(),
                          query.limit(),
                          countPublishedByTagSlug(tagSlug));
    }

    public List<Post> findPublishedFeedByAuthor(long authorId, int limit) {
        return attachLatestPublications(entityManager.createQuery("""
                                                                  SELECT DISTINCT p FROM Post p
                                                                  JOIN FETCH p.blog b
                                                                  JOIN FETCH b.owner o
                                                                  LEFT JOIN FETCH p.tags
                                                                  WHERE b.owner.id = :authorId AND p.published = true
                                                                  ORDER BY p.publishedAt DESC NULLS LAST, p.id DESC
                                                                  """, Post.class)
                                                     .setParameter("authorId", authorId)
                                                     .setMaxResults(limit)
                                                     .getResultList());
    }

    public List<Post> findPublishedFeedByBlog(long blogId, int limit) {
        return attachLatestPublications(entityManager.createQuery("""
                                                                  SELECT DISTINCT p FROM Post p
                                                                  JOIN FETCH p.blog b
                                                                  JOIN FETCH b.owner o
                                                                  LEFT JOIN FETCH p.tags
                                                                  WHERE b.id = :blogId AND p.published = true
                                                                  ORDER BY p.publishedAt DESC NULLS LAST, p.id DESC
                                                                  """, Post.class)
                                                     .setParameter("blogId", blogId)
                                                     .setMaxResults(limit)
                                                     .getResultList());
    }

    public List<Post> findPublishedFeedBySerie(long serieId, int limit) {
        return attachLatestPublications(entityManager.createQuery("""
                                                                  SELECT DISTINCT p FROM Post p
                                                                  JOIN FETCH p.blog b
                                                                  JOIN FETCH b.owner o
                                                                  LEFT JOIN FETCH p.tags
                                                                  WHERE p.serie.id = :serieId AND p.published = true
                                                                  ORDER BY p.publishedAt DESC NULLS LAST, p.id DESC
                                                                  """, Post.class)
                                                     .setParameter("serieId", serieId)
                                                     .setMaxResults(limit)
                                                     .getResultList());
    }

    public List<Post> findPublishedFeedByTagSlug(String tagSlug, int limit) {
        return attachLatestPublications(entityManager.createQuery("""
                                                                  SELECT DISTINCT p FROM Post p
                                                                  JOIN FETCH p.blog b
                                                                  JOIN FETCH b.owner o
                                                                  LEFT JOIN FETCH p.tags
                                                                  WHERE p.published = true AND
                                                                        EXISTS (SELECT 1 FROM Post p2 JOIN p2.tags t
                                                                                WHERE p2.id = p.id AND t.slug = :slug)
                                                                  ORDER BY p.publishedAt DESC NULLS LAST, p.id DESC
                                                                  """, Post.class)
                                                     .setParameter("slug", tagSlug)
                                                     .setMaxResults(limit)
                                                     .getResultList());
    }

    public List<Post> findPublishedFeedGlobal(int limit) {
        return attachLatestPublications(entityManager.createQuery("""
                                                                  SELECT DISTINCT p FROM Post p
                                                                  JOIN FETCH p.blog b
                                                                  JOIN FETCH b.owner o
                                                                  LEFT JOIN FETCH p.tags
                                                                  WHERE p.published = true
                                                                  ORDER BY p.publishedAt DESC NULLS LAST, p.id DESC
                                                                  """, Post.class)
                                                     .setMaxResults(limit)
                                                     .getResultList());
    }

    public List<Post> findPublishedFeedMainBlogByOwner(long ownerId, int limit) {
        return attachLatestPublications(entityManager.createQuery("""
                                                                  SELECT DISTINCT p FROM Post p
                                                                  JOIN FETCH p.blog b
                                                                  JOIN FETCH b.owner o
                                                                  LEFT JOIN FETCH p.tags
                                                                  WHERE b.owner.id = :ownerId AND b.main = true AND p.published = true
                                                                  ORDER BY p.publishedAt DESC NULLS LAST, p.id DESC
                                                                  """, Post.class)
                                                     .setParameter("ownerId", ownerId)
                                                     .setMaxResults(limit)
                                                     .getResultList());
    }

    public List<Post> findRecentByAuthorAndPublished(long authorId, boolean published, int limit) {
        return entityManager.createQuery("""
                                         SELECT DISTINCT p FROM Post p
                                         LEFT JOIN FETCH p.tags
                                         WHERE p.blog.owner.id = :authorId
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
        if (post.getId() == null) {
            entityManager.persist(post);
        }
        entityManager.flush();
        logger.info("Updated post: {}", post.getSlug());
        return post;
    }

    @Transactional
    public Post saveFromGit(Post post) {
        entityManager.persist(post);
        logger.info("Git import persisted new post slug={}", post.getSlug());
        return post;
    }

    public Page<Post> search(String term, PageQuery query) {
        return new Page<>(attachLatestPublications(entityManager.createQuery("""
                                                                             SELECT DISTINCT p FROM Post p
                                                                             JOIN FETCH p.blog b
                                                                             JOIN FETCH b.owner o
                                                                             LEFT JOIN FETCH p.tags
                                                                             WHERE p.published = true
                                                                             AND EXISTS (
                                                                                 SELECT 1 FROM PostPublication pub
                                                                                 WHERE pub.post.id = p.id
                                                                                 AND (LOWER(pub.title) LIKE LOWER(:query)
                                                                                     OR LOWER(pub.description) LIKE LOWER(:query)
                                                                                     OR LOWER(pub.content) LIKE LOWER(:query))
                                                                             )
                                                                             ORDER BY p.publishedAt DESC
                                                                             """, Post.class)
                                                                .setParameter("query", "%%%s%%".formatted(term))
                                                                .setMaxResults(query.maxResults())
                                                                .setFirstResult(query.skip())
                                                                .getResultList()),
                          query.page(),
                          query.limit(),
                          countSearch(term));
    }

    public boolean slugExists(Long blogId, String slug, Long excludeId) {
        return entityManager.createQuery("""
                                         SELECT COUNT(p)
                                         FROM Post p
                                         WHERE p.slug = :slug AND
                                               p.blog.id = :blogId AND
                                               (:excludeId IS NULL OR p.id != :excludeId)
                                         """, Long.class)
                            .setParameter("slug", slug)
                            .setParameter("excludeId", excludeId)
                            .setParameter("blogId", blogId)
                            .getSingleResult() > 0;
    }

}