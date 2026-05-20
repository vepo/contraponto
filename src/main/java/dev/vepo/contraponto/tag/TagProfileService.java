package dev.vepo.contraponto.tag;

import java.util.ArrayList;
import java.util.List;

import dev.vepo.contraponto.user.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class TagProfileService {

    private static final String PARAM_OWNER_ID = "ownerId";
    private static final String PARAM_BLOG_ID = "blogId";
    private static final String PARAM_TAG_SLUG = "tagSlug";
    private static final String PARAM_LIMIT = "limit";

    private final EntityManager entityManager;
    private final TagRepository tagRepository;

    @Inject
    public TagProfileService(EntityManager entityManager, TagRepository tagRepository) {
        this.entityManager = entityManager;
        this.tagRepository = tagRepository;
    }

    public long countDistinctAuthorsForTag(String tagSlug) {
        Number count = (Number) entityManager.createNativeQuery("""
                                                                SELECT COUNT(DISTINCT u.id)
                                                                FROM tb_posts p
                                                                JOIN tb_blogs b ON b.id = p.blog_id
                                                                JOIN tb_post_tags pt ON pt.post_id = p.id
                                                                JOIN tb_tags t ON t.id = pt.tag_id
                                                                JOIN tb_users u ON u.id = b.owner_id
                                                                WHERE p.published = TRUE
                                                                  AND b.active = TRUE
                                                                  AND u.active = TRUE
                                                                  AND t.slug = :tagSlug
                                                                """)
                                             .setParameter(PARAM_TAG_SLUG, tagSlug)
                                             .getSingleResult();
        return count.longValue();
    }

    private List<TagUsage> loadTagUsages(String sql, String paramName, long paramValue, int limit) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(sql + " LIMIT :limit")
                                           .setParameter(paramName, paramValue)
                                           .setParameter(PARAM_LIMIT, limit)
                                           .getResultList();
        List<TagUsage> result = new ArrayList<>();
        for (Object[] row : rows) {
            long tagId = ((Number) row[0]).longValue();
            long count = ((Number) row[1]).longValue();
            tagRepository.findById(tagId).ifPresent(tag -> result.add(new TagUsage(tag, count)));
        }
        return result;
    }

    public List<AuthorTagUsage> mainAuthorsForTag(String tagSlug, int limit) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                                                              SELECT u.id AS entity_id, COUNT(DISTINCT p.id) AS cnt
                                                              FROM tb_posts p
                                                              JOIN tb_blogs b ON b.id = p.blog_id
                                                              JOIN tb_post_tags pt ON pt.post_id = p.id
                                                              JOIN tb_tags t ON t.id = pt.tag_id
                                                              JOIN tb_users u ON u.id = b.owner_id
                                                              WHERE p.published = TRUE
                                                                AND b.active = TRUE
                                                                AND u.active = TRUE
                                                                AND t.slug = :tagSlug
                                                              GROUP BY u.id
                                                              ORDER BY cnt DESC, u.id ASC
                                                              LIMIT :limit
                                                              """)
                                           .setParameter(PARAM_TAG_SLUG, tagSlug)
                                           .setParameter(PARAM_LIMIT, limit)
                                           .getResultList();
        List<AuthorTagUsage> result = new ArrayList<>();
        for (Object[] row : rows) {
            long userId = ((Number) row[0]).longValue();
            long count = ((Number) row[1]).longValue();
            User author = entityManager.find(User.class, userId);
            if (author != null) {
                result.add(new AuthorTagUsage(author, count));
            }
        }
        return result;
    }

    public List<TagUsage> topTagsForAuthor(long ownerId, int limit) {
        return loadTagUsages("""
                             SELECT t.id AS entity_id, COUNT(DISTINCT p.id) AS cnt
                             FROM tb_posts p
                             JOIN tb_blogs b ON b.id = p.blog_id
                             JOIN tb_post_tags pt ON pt.post_id = p.id
                             JOIN tb_tags t ON t.id = pt.tag_id
                             WHERE p.published = TRUE
                               AND b.active = TRUE
                               AND b.owner_id = :ownerId
                             GROUP BY t.id
                             ORDER BY cnt DESC, t.id ASC
                             """,
                             PARAM_OWNER_ID,
                             ownerId,
                             limit);
    }

    public List<TagUsage> topTagsForBlog(long blogId, int limit) {
        return loadTagUsages("""
                             SELECT t.id AS entity_id, COUNT(DISTINCT p.id) AS cnt
                             FROM tb_posts p
                             JOIN tb_blogs b ON b.id = p.blog_id
                             JOIN tb_post_tags pt ON pt.post_id = p.id
                             JOIN tb_tags t ON t.id = pt.tag_id
                             WHERE p.published = TRUE
                               AND b.active = TRUE
                               AND b.id = :blogId
                             GROUP BY t.id
                             ORDER BY cnt DESC, t.id ASC
                             """,
                             PARAM_BLOG_ID,
                             blogId,
                             limit);
    }
}
