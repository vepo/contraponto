package dev.vepo.contraponto.auth;

import java.util.Optional;

import dev.vepo.contraponto.user.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class UserAccountTokenRepository {

    private final EntityManager entityManager;

    @Inject
    public UserAccountTokenRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<UserAccountToken> findValidByHash(String tokenHash) {
        return entityManager.createQuery("""
                                         FROM UserAccountToken t
                                         WHERE t.tokenHash = :hash
                                           AND t.usedAt IS NULL
                                           AND t.expiresAt > CURRENT_TIMESTAMP
                                         """, UserAccountToken.class)
                            .setParameter("hash", tokenHash)
                            .getResultStream()
                            .findFirst();
    }

    @Transactional
    public void invalidateActiveForUser(User user, UserAccountTokenType type) {
        entityManager.createQuery("""
                                  UPDATE UserAccountToken t
                                  SET t.usedAt = CURRENT_TIMESTAMP
                                  WHERE t.user = :user
                                    AND t.type = :type
                                    AND t.usedAt IS NULL
                                  """)
                     .setParameter("user", user)
                     .setParameter("type", type)
                     .executeUpdate();
    }

    @Transactional
    public void markUsed(UserAccountToken token) {
        token.markUsed();
        entityManager.merge(token);
    }

    @Transactional
    public void persist(UserAccountToken token) {
        entityManager.persist(token);
    }
}
