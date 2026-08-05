package dev.vepo.contraponto.reddit.account;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class UserRedditAccountRepository {

    private final EntityManager entityManager;

    @Inject
    public UserRedditAccountRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public UserRedditAccount create(UserRedditAccount account) {
        entityManager.persist(account);
        return account;
    }

    @Transactional
    public void delete(UserRedditAccount account) {
        entityManager.remove(entityManager.contains(account) ? account : entityManager.merge(account));
    }

    public Optional<UserRedditAccount> findByUserId(long userId) {
        return entityManager.createQuery("""
                                         SELECT a FROM UserRedditAccount a
                                         JOIN FETCH a.user
                                         WHERE a.user.id = :userId
                                         """, UserRedditAccount.class)
                            .setParameter("userId", userId)
                            .getResultStream()
                            .findFirst();
    }

    @Transactional
    public UserRedditAccount update(UserRedditAccount account) {
        return entityManager.merge(account);
    }
}
