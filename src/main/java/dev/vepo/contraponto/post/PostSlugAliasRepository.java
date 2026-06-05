package dev.vepo.contraponto.post;

import java.time.LocalDateTime;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class PostSlugAliasRepository {

    private final EntityManager entityManager;

    public PostSlugAliasRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<Long> findPostIdByBlogAndSlug(long blogId, String slug) {
        return entityManager.createQuery("""
                                         SELECT a.post.id FROM PostSlugAlias a
                                         WHERE a.blog.id = :blogId AND a.slug = :slug
                                         """, Long.class)
                            .setParameter("blogId", blogId)
                            .setParameter("slug", slug)
                            .getResultStream()
                            .findFirst();
    }

    @Transactional
    public void saveIfAbsent(Post post, String slug) {
        if (slug == null || slug.isBlank()) {
            return;
        }
        if (slug.equals(post.getSlug())) {
            return;
        }
        boolean exists = entityManager.createQuery("""
                                                   SELECT COUNT(a) FROM PostSlugAlias a
                                                   WHERE a.blog.id = :blogId AND a.slug = :slug
                                                   """, Long.class)
                                      .setParameter("blogId", post.getBlog().getId())
                                      .setParameter("slug", slug)
                                      .getSingleResult() > 0;
        if (exists) {
            return;
        }
        var alias = new PostSlugAlias();
        alias.setPost(post);
        alias.setBlog(post.getBlog());
        alias.setSlug(slug);
        alias.setCreatedAt(LocalDateTime.now());
        entityManager.persist(alias);
    }
}
