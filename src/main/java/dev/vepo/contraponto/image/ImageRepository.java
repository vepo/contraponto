package dev.vepo.contraponto.image;

import java.util.List;
import java.util.Optional;

import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
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

    public Optional<Image> findByUuidAndBlogId(String uuid, long blogId) {
        return entityManager.createQuery("FROM Image WHERE uuid = :uuid AND active = true AND blog.id = :blogId", Image.class)
                            .setParameter("uuid", uuid)
                            .setParameter("blogId", blogId)
                            .getResultStream()
                            .findFirst();
    }

    public Page<Image> findPageByBlogId(long blogId, PageQuery query) {
        long total = entityManager.createQuery("SELECT COUNT(i) FROM Image i WHERE i.blog.id = :blogId AND i.active = true", Long.class)
                                  .setParameter("blogId", blogId)
                                  .getSingleResult();
        List<Image> data = entityManager.createQuery("""
                                                     SELECT i FROM Image i
                                                     WHERE i.blog.id = :blogId AND i.active = true
                                                     ORDER BY i.createdAt DESC
                                                     """, Image.class)
                                        .setParameter("blogId", blogId)
                                        .setFirstResult(query.skip())
                                        .setMaxResults(query.limit())
                                        .getResultList();
        return new Page<>(data, query.page(), query.limit(), total);
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
