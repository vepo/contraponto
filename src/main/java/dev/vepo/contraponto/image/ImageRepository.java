package dev.vepo.contraponto.image;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ImageRepository {

    private final EntityManager entityManager;

    @Inject
    public ImageRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<Image> findById(Long id) {
        return Optional.ofNullable(entityManager.find(Image.class, id));
    }

    public Optional<Image> findByUuid(String uuid) {
        return entityManager.createQuery("FROM Image WHERE uuid = :uuid AND active = true", Image.class)
                            .setParameter("uuid", uuid)
                            .getResultStream()
                            .findFirst();
    }

    @Transactional
    public Image save(Image image) {
        entityManager.persist(image);
        return image;
    }

    @Transactional
    public void softDelete(String uuid) {
        entityManager.createQuery("UPDATE Image SET active = false WHERE uuid = :uuid")
                     .setParameter("uuid", uuid)
                     .executeUpdate();
    }

    @Transactional
    public Image update(Image image) {
        return entityManager.merge(image);
    }
}