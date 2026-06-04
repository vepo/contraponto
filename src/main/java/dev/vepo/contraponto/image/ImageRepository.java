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

    private static final String PARAM_OWNER_ID = "ownerId";
    private static final String PARAM_SEARCH = "search";

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

    public Optional<Image> findByUuidAndOwnerId(String uuid, long ownerId) {
        return entityManager.createQuery("""
                                         FROM Image
                                         WHERE uuid = :uuid AND active = true AND owner.id = :ownerId
                                         """, Image.class)
                            .setParameter("uuid", uuid)
                            .setParameter(PARAM_OWNER_ID, ownerId)
                            .getResultStream()
                            .findFirst();
    }

    public Page<Image> findPageByOwnerId(long ownerId, String searchQuery, PageQuery query) {
        boolean hasSearch = searchQuery != null && !searchQuery.isBlank();
        String searchTerm = hasSearch ? "%" + searchQuery.trim().toLowerCase() + "%" : null;

        String countJpql = """
                           SELECT COUNT(i) FROM Image i
                           WHERE i.owner.id = :ownerId AND i.active = true
                           """;
        String dataJpql = """
                          SELECT i FROM Image i
                          WHERE i.owner.id = :ownerId AND i.active = true
                          """;
        if (hasSearch) {
            String searchClause = """
                                  AND (
                                      LOWER(i.altText) LIKE :search
                                      OR LOWER(i.filename) LIKE :search
                                      OR LOWER(i.gitAssetRelativePath) LIKE :search
                                  )
                                  """;
            countJpql += searchClause;
            dataJpql += searchClause;
        }
        dataJpql += " ORDER BY i.createdAt DESC";

        var countQuery = entityManager.createQuery(countJpql, Long.class).setParameter(PARAM_OWNER_ID, ownerId);
        var dataQuery = entityManager.createQuery(dataJpql, Image.class)
                                     .setParameter(PARAM_OWNER_ID, ownerId)
                                     .setFirstResult(query.skip())
                                     .setMaxResults(query.limit());
        if (hasSearch) {
            countQuery.setParameter(PARAM_SEARCH, searchTerm);
            dataQuery.setParameter(PARAM_SEARCH, searchTerm);
        }

        long total = countQuery.getSingleResult();
        List<Image> data = dataQuery.getResultList();
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
