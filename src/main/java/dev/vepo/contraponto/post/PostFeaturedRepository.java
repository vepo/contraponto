package dev.vepo.contraponto.post;

import java.util.List;

import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class PostFeaturedRepository {

    private final EntityManager entityManager;
    private final PostPublicationRepository publicationRepository;

    @Inject
    public PostFeaturedRepository(EntityManager entityManager, PostPublicationRepository publicationRepository) {
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

    private long countFeatured() {
        return entityManager.createQuery("""
                                         SELECT COUNT(p)
                                         FROM Post p
                                         WHERE p.published = true AND
                                               p.featured = true
                                         """, Long.class)
                            .getSingleResult();
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
}
