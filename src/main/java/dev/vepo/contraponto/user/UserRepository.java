package dev.vepo.contraponto.user;

import java.util.List;
import java.util.Optional;

import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class UserRepository {

    private final EntityManager entityManager;

    @Inject
    public UserRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public boolean existsByEmail(String email) {
        return existsByEmail(email, null);
    }

    public boolean existsByEmail(String email, Long excludeUserId) {
        var cb = entityManager.getCriteriaBuilder();
        var criteria = cb.createQuery(Long.class);
        var root = criteria.from(User.class);
        var predicates = cb.or(cb.equal(root.get("email"), email), cb.equal(root.get("pendingEmail"), email));
        if (excludeUserId != null) {
            predicates = cb.and(predicates, cb.notEqual(root.get("id"), excludeUserId));
        }
        criteria.select(cb.count(root));
        criteria.where(predicates);
        return entityManager.createQuery(criteria).getSingleResult() > 0;
    }

    public boolean existsByUsername(String username) {
        return existsByUsername(username, null);
    }

    public boolean existsByUsername(String username, Long excludeUserId) {
        var cb = entityManager.getCriteriaBuilder();
        var criteria = cb.createQuery(Long.class);
        var root = criteria.from(User.class);
        var predicates = cb.equal(root.get("username"), username);
        if (excludeUserId != null) {
            predicates = cb.and(predicates, cb.notEqual(root.get("id"), excludeUserId));
        }
        criteria.select(cb.count(root));
        criteria.where(predicates);
        return entityManager.createQuery(criteria).getSingleResult() > 0;
    }

    public List<String> findAdministratorEmails() {
        return entityManager.createQuery("""
                                         SELECT DISTINCT u.email FROM User u
                                         JOIN u.roles r
                                         WHERE u.active = true AND r IN (:roles)
                                         ORDER BY u.email ASC
                                         """, String.class)
                            .setParameter("roles", List.of(Role.ADMIN, Role.USER_ADMINISTRATOR))
                            .getResultList();
    }

    public List<User> findAuthorsWithPublishedPosts() {
        return entityManager.createQuery("""
                                         SELECT DISTINCT u FROM User u
                                         JOIN Blog b ON b.owner = u
                                         WHERE b.active = true AND
                                               EXISTS (
                                                   SELECT 1 FROM Post p
                                                   WHERE p.blog = b AND p.published = true
                                               )
                                         ORDER BY u.username ASC
                                         """, User.class)
                            .getResultList();
    }

    public List<User> findAuthorsWithPublishedPostsForDirectory() {
        return entityManager.createQuery("""
                                         SELECT DISTINCT u FROM User u
                                         LEFT JOIN FETCH u.profilePicture
                                         JOIN Blog b ON b.owner = u
                                         WHERE b.active = true AND
                                               EXISTS (
                                                   SELECT 1 FROM Post p
                                                   WHERE p.blog = b AND p.published = true
                                               )
                                         ORDER BY u.name ASC, u.username ASC
                                         """, User.class)
                            .getResultList();
    }

    public Optional<User> findByEmail(String email) {
        return entityManager.createQuery("FROM User WHERE email = :email", User.class)
                            .setParameter("email", email)
                            .getResultStream()
                            .findFirst();
    }

    public Optional<User> findById(long userId) {
        return Optional.ofNullable(entityManager.find(User.class, userId));
    }

    public Optional<User> findByUsername(String username) {
        return entityManager.createQuery("FROM User WHERE username = :username", User.class)
                            .setParameter("username", username)
                            .getResultStream()
                            .findFirst();
    }

    public Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
        return entityManager.createQuery("FROM User WHERE username = :login OR email = :login", User.class)
                            .setParameter("login", usernameOrEmail)
                            .getResultStream()
                            .findFirst();
    }

    public Page<User> findPageForManagement(PageQuery query) {
        long total = entityManager.createQuery("SELECT COUNT(u) FROM User u", Long.class)
                                  .getSingleResult();
        var data = entityManager.createQuery("""
                                             SELECT u FROM User u
                                             ORDER BY u.name, u.username
                                             """, User.class)
                                .setFirstResult(query.skip())
                                .setMaxResults(query.maxResults())
                                .getResultList();
        return new Page<>(data, query.page(), query.limit(), total);
    }

    public Optional<User> findPublicAuthorByUsername(String username) {
        return entityManager.createQuery("""
                                         SELECT DISTINCT u FROM User u
                                         JOIN Blog b ON b.owner = u
                                         WHERE u.username = :username AND
                                               u.active = true AND
                                               b.active = true AND
                                               EXISTS (
                                                   SELECT 1 FROM Post p
                                                   WHERE p.blog = b AND p.published = true
                                               )
                                         """, User.class)
                            .setParameter("username", username)
                            .getResultStream()
                            .findFirst();
    }

    public List<User> listAllForManagement() {
        return entityManager.createQuery("""
                                         SELECT u FROM User u
                                         ORDER BY u.name, u.username
                                         """, User.class)
                            .getResultList();
    }

    @Transactional
    public User save(User user) {
        entityManager.persist(user);
        return user;
    }

    @Transactional
    public User update(User user) {
        return entityManager.merge(user);
    }
}
