package dev.vepo.contraponto.image;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ImageDependencyRepository {

    private static final String PARAM_IMAGE_ID = "imageId";
    private static final String PARAM_BLOG_ID = "blogId";

    private final EntityManager entityManager;

    @Inject
    public ImageDependencyRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public void deleteCustomPageDependencies(long customPageId) {
        entityManager.createQuery("DELETE FROM CustomPageImageDependency d WHERE d.customPage.id = :customPageId")
                     .setParameter("customPageId", customPageId)
                     .executeUpdate();
    }

    @Transactional
    public void deletePostDependencies(long postId) {
        entityManager.createQuery("DELETE FROM PostImageDependency d WHERE d.post.id = :postId")
                     .setParameter("postId", postId)
                     .executeUpdate();
    }

    @Transactional
    public void deletePublicationDependencies(long publicationId) {
        entityManager.createQuery("DELETE FROM PostPublicationImageDependency d WHERE d.publication.id = :publicationId")
                     .setParameter("publicationId", publicationId)
                     .executeUpdate();
    }

    @SuppressWarnings("unchecked")
    public List<ImageUsageRow> findUsagesForImage(long imageId, long blogId) {
        var postUsages = entityManager.createNativeQuery("""
                                                         SELECT p.id, p.title, d.role, 'POST' AS kind
                                                         FROM tb_post_image_dependencies d
                                                         JOIN tb_posts p ON p.id = d.post_id
                                                         WHERE d.image_id = :imageId AND p.blog_id = :blogId
                                                         """)
                                      .setParameter(PARAM_IMAGE_ID, imageId)
                                      .setParameter(PARAM_BLOG_ID, blogId)
                                      .getResultList();
        var pageUsages = entityManager.createNativeQuery("""
                                                         SELECT cp.id, cp.title, d.role, 'CUSTOM_PAGE' AS kind
                                                         FROM tb_custom_page_image_dependencies d
                                                         JOIN tb_custom_pages cp ON cp.id = d.custom_page_id
                                                         WHERE d.image_id = :imageId AND cp.blog_id = :blogId
                                                         """)
                                      .setParameter(PARAM_IMAGE_ID, imageId)
                                      .setParameter(PARAM_BLOG_ID, blogId)
                                      .getResultList();
        var rows = new java.util.ArrayList<ImageUsageRow>();
        for (Object row : postUsages) {
            Object[] cols = (Object[]) row;
            rows.add(new ImageUsageRow(((Number) cols[0]).longValue(),
                                       (String) cols[1],
                                       ImageRole.valueOf((String) cols[2]),
                                       ImageUsageKind.POST));
        }
        for (Object row : pageUsages) {
            Object[] cols = (Object[]) row;
            rows.add(new ImageUsageRow(((Number) cols[0]).longValue(),
                                       (String) cols[1],
                                       ImageRole.valueOf((String) cols[2]),
                                       ImageUsageKind.CUSTOM_PAGE));
        }
        return rows;
    }

    public boolean isReferenced(long imageId) {
        long postDeps = entityManager.createQuery("SELECT COUNT(d) FROM PostImageDependency d WHERE d.image.id = :imageId", Long.class)
                                     .setParameter(PARAM_IMAGE_ID, imageId)
                                     .getSingleResult();
        if (postDeps > 0) {
            return true;
        }
        long pubDeps = entityManager.createQuery("SELECT COUNT(d) FROM PostPublicationImageDependency d WHERE d.image.id = :imageId", Long.class)
                                    .setParameter(PARAM_IMAGE_ID, imageId)
                                    .getSingleResult();
        if (pubDeps > 0) {
            return true;
        }
        long pageDeps = entityManager.createQuery("SELECT COUNT(d) FROM CustomPageImageDependency d WHERE d.image.id = :imageId", Long.class)
                                     .setParameter(PARAM_IMAGE_ID, imageId)
                                     .getSingleResult();
        if (pageDeps > 0) {
            return true;
        }
        long coverPosts = entityManager.createQuery("SELECT COUNT(p) FROM Post p WHERE p.cover.id = :imageId", Long.class)
                                       .setParameter(PARAM_IMAGE_ID, imageId)
                                       .getSingleResult();
        if (coverPosts > 0) {
            return true;
        }
        return entityManager.createQuery("SELECT COUNT(p) FROM PostPublication p WHERE p.cover.id = :imageId", Long.class)
                            .setParameter(PARAM_IMAGE_ID, imageId)
                            .getSingleResult() > 0;
    }

    public void persistCustomPageDependency(CustomPageImageDependency dependency) {
        entityManager.persist(dependency);
    }

    public void persistPostDependency(PostImageDependency dependency) {
        entityManager.persist(dependency);
    }

    public void persistPublicationDependency(PostPublicationImageDependency dependency) {
        entityManager.persist(dependency);
    }
}
