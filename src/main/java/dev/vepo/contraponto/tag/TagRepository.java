package dev.vepo.contraponto.tag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import dev.vepo.contraponto.shared.Slug;
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

    public List<Tag> findByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return entityManager.createQuery("FROM Tag t WHERE t.id IN :ids", Tag.class)
                            .setParameter("ids", ids)
                            .getResultList();
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
        var trimmed = label.trim();
        var slug = Slug.slugify(trimmed);
        if (slug.isEmpty()) {
            throw new IllegalArgumentException("Invalid tag label");
        }
        return findBySlug(slug).orElseGet(() -> {
            var created = new Tag(slug, trimmed, null);
            entityManager.persist(created);
            return created;
        });
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

    public Map<Long, List<TagUsage>> findTopTagUsagesForBlogIds(Collection<Long> blogIds, int limitPerBlog) {
        if (blogIds == null || blogIds.isEmpty() || limitPerBlog <= 0) {
            return Map.of();
        }
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createQuery("""
                                                        SELECT b.id, t.id, COUNT(DISTINCT p.id)
                                                        FROM Post p
                                                        JOIN p.blog b
                                                        JOIN p.tags t
                                                        WHERE p.published = true
                                                          AND b.active = true
                                                          AND b.id IN :blogIds
                                                        GROUP BY b.id, t.id
                                                        ORDER BY b.id ASC, COUNT(DISTINCT p.id) DESC, t.id ASC
                                                        """)
                                           .setParameter("blogIds", blogIds)
                                           .getResultList();
        var rankedByBlog = new LinkedHashMap<Long, List<Object[]>>();
        for (var row : rows) {
            long blogId = ((Number) row[0]).longValue();
            var blogRows = rankedByBlog.computeIfAbsent(blogId, ignored -> new ArrayList<>());
            if (blogRows.size() < limitPerBlog) {
                blogRows.add(row);
            }
        }
        var tagIds = rankedByBlog.values()
                                 .stream()
                                 .flatMap(List::stream)
                                 .map(row -> ((Number) row[1]).longValue())
                                 .collect(Collectors.toSet());
        var tagsById = findByIds(tagIds).stream().collect(Collectors.toMap(Tag::getId, tag -> tag));
        var result = new HashMap<Long, List<TagUsage>>();
        rankedByBlog.forEach((blogId, blogRows) -> {
            var usages = new ArrayList<TagUsage>();
            for (var row : blogRows) {
                long tagId = ((Number) row[1]).longValue();
                long count = ((Number) row[2]).longValue();
                var tag = tagsById.get(tagId);
                if (tag != null) {
                    usages.add(new TagUsage(tag, count));
                }
            }
            result.put(blogId, usages);
        });
        return result;
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
