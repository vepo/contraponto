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
        var cb = entityManager.getCriteriaBuilder();

        // Count query
        var countCriteria = cb.createQuery(Long.class);
        var countRoot = countCriteria.from(Image.class);
        var countPredicates = cb.and(
                                     cb.equal(countRoot.get("owner").get("id"), ownerId),
                                     cb.isTrue(countRoot.get("active")));

        boolean hasSearch = searchQuery != null && !searchQuery.isBlank();
        String searchTerm = hasSearch ? "%%%s%%".formatted(searchQuery.trim().toLowerCase()) : null;

        if (hasSearch) {
            var searchPredicate = cb.or(
                                        cb.like(cb.lower(countRoot.get("altText")), searchTerm),
                                        cb.like(cb.lower(countRoot.get("filename")), searchTerm),
                                        cb.like(cb.lower(countRoot.get("gitAssetRelativePath")), searchTerm));
            countPredicates = cb.and(countPredicates, searchPredicate);
        }

        countCriteria.select(cb.count(countRoot));
        countCriteria.where(countPredicates);

        long total = entityManager.createQuery(countCriteria).getSingleResult();

        // Data query
        var dataCriteria = cb.createQuery(Image.class);
        var dataRoot = dataCriteria.from(Image.class);
        var dataPredicates = cb.and(
                                    cb.equal(dataRoot.get("owner").get("id"), ownerId),
                                    cb.isTrue(dataRoot.get("active")));

        if (hasSearch) {
            var searchPredicate = cb.or(
                                        cb.like(cb.lower(dataRoot.get("altText")), searchTerm),
                                        cb.like(cb.lower(dataRoot.get("filename")), searchTerm),
                                        cb.like(cb.lower(dataRoot.get("gitAssetRelativePath")), searchTerm));
            dataPredicates = cb.and(dataPredicates, searchPredicate);
        }

        dataCriteria.select(dataRoot);
        dataCriteria.where(dataPredicates);
        dataCriteria.orderBy(cb.desc(dataRoot.get("createdAt")));

        List<Image> data = entityManager.createQuery(dataCriteria)
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
