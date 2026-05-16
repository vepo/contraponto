package dev.vepo.contraponto.tag;

import java.util.List;
import java.util.Optional;

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
        String pattern = "%" + prefix.toLowerCase() + "%";
        return entityManager.createQuery("""
                                         SELECT t.name FROM Tag t
                                         WHERE LOWER(t.name) LIKE :pattern OR LOWER(t.slug) LIKE :pattern
                                         ORDER BY t.name ASC
                                         """, String.class)
                            .setParameter("pattern", pattern)
                            .setMaxResults(limit)
                            .getResultList();
    }
}
