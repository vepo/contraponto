package dev.vepo.contraponto.custompage;

import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;

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
                                                                                      .map(page -> new Link(page.getSlug(), page.getTitle()))
                                                                                      .toList()))
                                         .toList();
            sectionsByPlacement.put(placement, sections);
        }
        logger.info("sections: {}", sectionsByPlacement);
        return new Links(sectionsByPlacement);
    }

    public Optional<CustomPage> findBySlug(String slug) {
        return entityManager.createQuery("""
                                         FROM CustomPage
                                         WHERE published = true AND
                                               slug = :slug AND
                                               blog IS NULL
                                         """, CustomPage.class)
                            .setParameter("slug", slug)
                            .setMaxResults(1)
                            .getResultStream()
                            .findFirst();
    }

    public Optional<CustomPage> findByUsernameAndSlug(String username, String slug) {
        return entityManager.createQuery("""
                                         FROM CustomPage
                                         WHERE published = true AND
                                               slug = :slug AND
                                               blog.owner.username = :username
                                         """, CustomPage.class)
                            .setParameter("slug", slug)
                            .setParameter("username", username)
                            .setMaxResults(1)
                            .getResultStream()
                            .findFirst();
    }

    private Stream<CustomPage> listBlogPages(String username) {
        return entityManager.createQuery("""
                                         SELECT cp FROM CustomPage cp
                                         LEFT JOIN cp.blog b
                                         WHERE cp.published = true AND
                                               (b IS NULL OR b.owner.username = :username)
                                         """, CustomPage.class)
                            .setParameter("username", username)
                            .getResultStream();
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

    public Links loadLinks(String username) {
        return buildLinks(listBlogPages(username));
    }

    public Links loadLinks(long blogId) {
        return buildLinks(listBlogPages(blogId));
    }

}
