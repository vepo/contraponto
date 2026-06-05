package dev.vepo.contraponto.tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class TagRepository {

    private final EntityManager entityManager;

    @Inject
    public TagRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public long countDistinctAuthorsForTag(String tagSlug) {
        return entityManager.createQuery("""
                                         SELECT COUNT(DISTINCT u.id)
                                         FROM Post p
                                         JOIN p.blog b
                                         JOIN p.tags t
                                         JOIN b.owner u
                                         WHERE p.published = true
                                           AND b.active = true
                                           AND u.active = true
                                           AND t.slug = :tagSlug
                                         """, Long.class)
                            .setParameter("tagSlug", tagSlug)
                            .getSingleResult();
    }

    public boolean existsOtherWithSlug(Long excludeId, String slug) {
        return entityManager.createQuery("""
                                         SELECT COUNT(t)
                                         FROM Tag t
                                         WHERE t.slug = :slug AND (:excludeId IS NULL OR t.id != :excludeId)
                                         """, Long.class)
                            .setParameter("slug", slug)
                            .setParameter("excludeId", excludeId)
                            .getSingleResult() > 0;
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findAuthorUsageRowsForTag(String tagSlug, int limit) {
        return entityManager.createQuery("""
                                         SELECT u.id, COUNT(DISTINCT p.id)
                                         FROM Post p
                                         JOIN p.blog b
                                         JOIN p.tags t
                                         JOIN b.owner u
                                         WHERE p.published = true
                                           AND b.active = true
                                           AND u.active = true
                                           AND t.slug = :tagSlug
                                         GROUP BY u.id
                                         ORDER BY COUNT(DISTINCT p.id) DESC, u.id ASC
                                         """)
                            .setParameter("tagSlug", tagSlug)
                            .setMaxResults(limit)
                            .getResultList();
    }

    public Optional<Tag> findById(Long id) {
        return Optional.ofNullable(entityManager.find(Tag.class, id));
    }

    public Optional<Tag> findBySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return Optional.empty();
        }
        return entityManager.createQuery("FROM Tag t WHERE t.slug = :slug", Tag.class)
                            .setParameter("slug", slug.trim())
                            .getResultStream()
                            .findFirst();
    }

    @Transactional
    public Tag findOrCreateByLabel(String label) {
        String trimmed = label.trim();
        String slug = TagSlug.slugify(trimmed);
        if (slug.isEmpty()) {
            throw new IllegalArgumentException("Invalid tag label");
        }
        Optional<Tag> existing = findBySlug(slug);
        if (existing.isPresent()) {
            return existing.get();
        }
        Tag created = new Tag(slug, trimmed, null);
        entityManager.persist(created);
        return created;
    }

    public Page<Tag> findPageForManagement(PageQuery query) {
        long total = entityManager.createQuery("SELECT COUNT(t) FROM Tag t", Long.class)
                                  .getSingleResult();
        var data = entityManager.createQuery("FROM Tag t ORDER BY t.name ASC", Tag.class)
                                .setFirstResult(query.skip())
                                .setMaxResults(query.maxResults())
                                .getResultList();
        return new Page<>(data, query.page(), query.limit(), total);
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> findTagUsageRows(Long ownerId, Long blogId, int limit) {
        var query = entityManager.createQuery("""
                                              SELECT t.id, COUNT(DISTINCT p.id)
                                              FROM Post p
                                              JOIN p.blog b
                                              JOIN p.tags t
                                              WHERE p.published = true
                                                AND b.active = true
                                                AND (:ownerId IS NULL OR b.owner.id = :ownerId)
                                                AND (:blogId IS NULL OR b.id = :blogId)
                                              GROUP BY t.id
                                              ORDER BY COUNT(DISTINCT p.id) DESC, t.id ASC
                                              """);
        query.setParameter("ownerId", ownerId);
        query.setParameter("blogId", blogId);
        return query.setMaxResults(limit).getResultList();
    }

    public List<TagUsage> findTopTagUsagesForAuthor(long ownerId, int limit) {
        return toTagUsages(findTagUsageRows(ownerId, null, limit));
    }

    public List<TagUsage> findTopTagUsagesForBlog(long blogId, int limit) {
        return toTagUsages(findTagUsageRows(null, blogId, limit));
    }

    public void flush() {
        entityManager.flush();
    }

    public List<Tag> listAllForManagement() {
        return entityManager.createQuery("FROM Tag t ORDER BY t.name ASC", Tag.class)
                            .getResultList();
    }

    @Transactional
    public Tag save(Tag tag) {
        if (tag.getId() == null) {
            entityManager.persist(tag);
        } else {
            entityManager.merge(tag);
        }
        return tag;
    }

    public List<String> suggestNames(String prefix, int limit) {
        if (prefix == null || prefix.isBlank()) {
            return entityManager.createQuery("""
                                             SELECT t.name FROM Tag t
                                             ORDER BY t.name ASC
                                             """, String.class)
                                .setMaxResults(limit)
                                .getResultList();
        }
        String pattern = "%%%s%%".formatted(prefix.toLowerCase());
        return entityManager.createQuery("""
                                         SELECT t.name FROM Tag t
                                         WHERE LOWER(t.name) LIKE :pattern OR LOWER(t.slug) LIKE :pattern
                                         ORDER BY t.name ASC
                                         """, String.class)
                            .setParameter("pattern", pattern)
                            .setMaxResults(limit)
                            .getResultList();
    }

    private List<TagUsage> toTagUsages(List<Object[]> rows) {
        var result = new ArrayList<TagUsage>();
        for (var row : rows) {
            long tagId = ((Number) row[0]).longValue();
            long count = ((Number) row[1]).longValue();
            findById(tagId).ifPresent(tag -> result.add(new TagUsage(tag, count)));
        }
        return result;
    }
}
