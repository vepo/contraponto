package dev.vepo.contraponto.auth;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class AccountEmailOutboxRepository {

    private final EntityManager entityManager;

    @Inject
    public AccountEmailOutboxRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public void delete(AccountEmailOutbox entry) {
        var managed = entityManager.contains(entry) ? entry : entityManager.merge(entry);
        entityManager.remove(managed);
    }

    public List<AccountEmailOutbox> findDue(LocalDateTime now, int limit) {
        return entityManager.createQuery("""
                                         SELECT e
                                         FROM AccountEmailOutbox e
                                         WHERE e.nextRetryAt <= :now
                                         ORDER BY e.nextRetryAt ASC, e.id ASC
                                         """, AccountEmailOutbox.class)
                            .setParameter("now", now)
                            .setMaxResults(limit)
                            .getResultList();
    }

    @Transactional
    public void persist(AccountEmailOutbox entry) {
        entityManager.persist(entry);
    }

    @Transactional
    public void recordFailure(AccountEmailOutbox entry, int attemptCount, LocalDateTime nextRetryAt, String lastError) {
        entry.applyRetryFailure(attemptCount, nextRetryAt, lastError);
        entityManager.merge(entry);
    }
}
