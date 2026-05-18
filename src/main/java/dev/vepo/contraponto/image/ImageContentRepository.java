package dev.vepo.contraponto.image;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ImageContentRepository {

    private final EntityManager entityManager;

    @Inject
    public ImageContentRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public void deleteByImageId(long imageId) {
        entityManager.createQuery("DELETE FROM ImageContent c WHERE c.imageId = :imageId")
                     .setParameter("imageId", imageId)
                     .executeUpdate();
    }

    public Optional<byte[]> findContentByImageId(long imageId) {
        return entityManager.createQuery("SELECT c.content FROM ImageContent c WHERE c.imageId = :imageId", byte[].class)
                            .setParameter("imageId", imageId)
                            .getResultStream()
                            .findFirst();
    }

    @Transactional
    public void save(Image image, byte[] content) {
        var imageContent = new ImageContent();
        imageContent.setImageId(image.getId());
        imageContent.setContent(content);
        entityManager.persist(imageContent);
    }
}
