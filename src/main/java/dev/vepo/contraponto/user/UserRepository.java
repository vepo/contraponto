package dev.vepo.contraponto.user;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class UserRepository {

    @Inject
    EntityManager entityManager;

    public Optional<User> findByEmail(String email) {
        return entityManager.createQuery("FROM User WHERE email = :email", User.class)
                            .setParameter("email", email)
                            .getResultStream()
                            .findFirst();
    }

    public Optional<User> findByUsername(String username) {
        return entityManager.createQuery("FROM User WHERE username = :username", User.class)
                            .setParameter("username", username)
                            .getResultStream()
                            .findFirst();
    }

    public Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
        return entityManager.createQuery("FROM User WHERE username = :usernameOrEmail OR email = :usernameOrEmail", User.class)
                            .setParameter("usernameOrEmail", usernameOrEmail)
                            .getResultStream()
                            .findFirst();
    }

    public boolean existsByEmail(String email) {
        return entityManager.createQuery("""
                                         SELECT id
                                         FROM User
                                         WHERE email = :email
                                         """, Long.class)
                            .setParameter("email", email)
                            .getResultStream()
                            .count() > 0l;
    }

    public boolean existsByUsername(String username) {
        return entityManager.createQuery("""
                                         SELECT id
                                         FROM User
                                         WHERE username = :username
                                         """, Long.class)
                            .setParameter("username", username)
                            .getResultStream()
                            .count() > 0l;
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

    public User findById(long userId) {
        return entityManager.find(User.class, userId);
    }
}