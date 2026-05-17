package dev.vepo.contraponto.custompage;

import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class CustomPageRepository {

    private static final Logger logger = LoggerFactory.getLogger(CustomPageRepository.class);
    private final EntityManager entityManager;

    public CustomPageRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    private Links buildLinks(Stream<CustomPage> pagesSource) {
        var pagesByPlacementAndSection = pagesSource.collect(Collectors.groupingBy(CustomPage::getPlacement,
                                                                                   Collectors.groupingBy(CustomPage::getSection)));

        var sectionsByPlacement = new EnumMap<PagePlacement, List<Section>>(PagePlacement.class);
        for (var placementEntry : pagesByPlacementAndSection.entrySet()) {
            var placement = placementEntry.getKey();
            var sections = placementEntry.getValue().entrySet().stream()
                                         .map(sectionEntry -> new Section(sectionEntry.getKey(),
                                                                          sectionEntry.getValue().stream()
                                                                                      .map(page -> new Link(CustomPagePaths.publicUrl(page), page.getTitle()))
                                                                                      .toList()))
                                         .toList();
            sectionsByPlacement.put(placement, sections);
        }
        logger.info("sections: {}", sectionsByPlacement);
        return new Links(sectionsByPlacement);
    }

    @Transactional
    public void delete(long id) {
        findByIdForManagement(id).ifPresent(entityManager::remove);
    }

    public boolean existsSlug(String slug, Long blogId, Long excludePageId) {
        var normalized = CustomPagePaths.storedSlug(slug);
        var query = new StringBuilder("""
                                      SELECT COUNT(cp) FROM CustomPage cp
                                      WHERE cp.slug = :slug
                                      """);
        if (blogId == null) {
            query.append(" AND cp.blog IS NULL");
        } else {
            query.append(" AND cp.blog.id = :blogId");
        }
        if (excludePageId != null) {
            query.append(" AND cp.id <> :excludePageId");
        }

        var typedQuery = entityManager.createQuery(query.toString(), Long.class).setParameter("slug", normalized);
        if (blogId != null) {
            typedQuery.setParameter("blogId", blogId);
        }
        if (excludePageId != null) {
            typedQuery.setParameter("excludePageId", excludePageId);
        }
        return typedQuery.getSingleResult() > 0;
    }

    public Optional<CustomPage> findByIdForManagement(long id) {
        return entityManager.createQuery("""
                                         SELECT cp FROM CustomPage cp
                                         LEFT JOIN FETCH cp.blog b
                                         LEFT JOIN FETCH b.owner
                                         WHERE cp.id = :id
                                         """, CustomPage.class)
                            .setParameter("id", id)
                            .getResultStream()
                            .findFirst();
    }

    private Optional<CustomPage> findByStoredSlug(String jpql, String slug) {
        return entityManager.createQuery(jpql, CustomPage.class)
                            .setParameter("slug", CustomPagePaths.storedSlug(slug))
                            .setMaxResults(1)
                            .getResultStream()
                            .findFirst();
    }

    public Optional<CustomPage> findByUsernameAndSlug(String username, String slug) {
        return entityManager.createQuery("""
                                         FROM CustomPage
                                         WHERE published = true AND
                                               slug = :slug AND
                                               blog.owner.username = :username AND
                                               blog.main = true
                                         """, CustomPage.class)
                            .setParameter("slug", CustomPagePaths.storedSlug(slug))
                            .setParameter("username", username)
                            .setMaxResults(1)
                            .getResultStream()
                            .findFirst();
    }

    public Optional<CustomPage> findByUsernameBlogSlugAndSlug(String username, String blogSlug, String slug) {
        return entityManager.createQuery("""
                                         FROM CustomPage
                                         WHERE published = true AND
                                               slug = :slug AND
                                               blog.owner.username = :username AND
                                               blog.slug = :blogSlug AND
                                               blog.main = false
                                         """, CustomPage.class)
                            .setParameter("slug", CustomPagePaths.storedSlug(slug))
                            .setParameter("username", username)
                            .setParameter("blogSlug", blogSlug)
                            .setMaxResults(1)
                            .getResultStream()
                            .findFirst();
    }

    public Optional<CustomPage> findGlobalBySlug(String slug) {
        return findByStoredSlug("""
                                FROM CustomPage
                                WHERE published = true AND
                                      slug = :slug AND
                                      blog IS NULL
                                """, slug);
    }

    public Page<CustomPageRow> findPageAllForManagement(PageQuery query) {
        long total = entityManager.createQuery("SELECT COUNT(cp) FROM CustomPage cp", Long.class)
                                  .getSingleResult();
        var data = entityManager.createQuery("""
                                             SELECT cp FROM CustomPage cp
                                             LEFT JOIN FETCH cp.blog b
                                             LEFT JOIN FETCH b.owner
                                             ORDER BY cp.blog.id NULLS FIRST, cp.title
                                             """, CustomPage.class)
                                .setFirstResult(query.skip())
                                .setMaxResults(query.maxResults())
                                .getResultStream()
                                .map(CustomPageRow::from)
                                .toList();
        return new Page<>(data, query.page(), query.limit(), total);
    }

    public Page<CustomPageRow> findPageByOwnerId(long ownerId, PageQuery query) {
        long total = entityManager.createQuery("""
                                               SELECT COUNT(cp) FROM CustomPage cp
                                               JOIN cp.blog b
                                               JOIN b.owner o
                                               WHERE o.id = :ownerId
                                               """, Long.class)
                                  .setParameter("ownerId", ownerId)
                                  .getSingleResult();
        var data = entityManager.createQuery("""
                                             SELECT cp FROM CustomPage cp
                                             JOIN FETCH cp.blog b
                                             JOIN FETCH b.owner o
                                             WHERE o.id = :ownerId
                                             ORDER BY cp.title
                                             """, CustomPage.class)
                                .setParameter("ownerId", ownerId)
                                .setFirstResult(query.skip())
                                .setMaxResults(query.maxResults())
                                .getResultStream()
                                .map(CustomPageRow::from)
                                .toList();
        return new Page<>(data, query.page(), query.limit(), total);
    }

    public List<CustomPageRow> listAllForManagement() {
        return entityManager.createQuery("""
                                         SELECT cp FROM CustomPage cp
                                         LEFT JOIN FETCH cp.blog b
                                         LEFT JOIN FETCH b.owner
                                         ORDER BY cp.blog.id NULLS FIRST, cp.title
                                         """, CustomPage.class)
                            .getResultStream()
                            .map(CustomPageRow::from)
                            .toList();
    }

    private Stream<CustomPage> listBlogPages(long blogId) {
        return entityManager.createQuery("""
                                         SELECT cp FROM CustomPage cp
                                         LEFT JOIN cp.blog b
                                         WHERE cp.published = true AND
                                               (b IS NULL OR b.id = :blogId)
                                         """, CustomPage.class)
                            .setParameter("blogId", blogId)
                            .getResultStream();
    }

    public List<CustomPageRow> listByOwnerId(long ownerId) {
        return entityManager.createQuery("""
                                         SELECT cp FROM CustomPage cp
                                         JOIN FETCH cp.blog b
                                         JOIN FETCH b.owner o
                                         WHERE o.id = :ownerId
                                         ORDER BY cp.title
                                         """, CustomPage.class)
                            .setParameter("ownerId", ownerId)
                            .getResultStream()
                            .map(CustomPageRow::from)
                            .toList();
    }

    private Stream<CustomPage> listMainPages() {
        return entityManager.createQuery("""
                                         FROM CustomPage
                                         WHERE published = true AND
                                               blog IS NULL
                                         """, CustomPage.class)
                            .getResultStream();
    }

    public Links loadLinks() {
        return buildLinks(listMainPages());
    }

    public Links loadLinks(long blogId) {
        return buildLinks(listBlogPages(blogId));
    }

    public CustomPage newPage(Blog blog) {
        return new CustomPage("/new-page", "New Page", "General", "<p></p>", PagePlacement.NONE, blog, false);
    }

    @Transactional
    public CustomPage save(CustomPage page) {
        if (page.getId() == null) {
            entityManager.persist(page);
            return page;
        }
        return entityManager.merge(page);
    }

}
