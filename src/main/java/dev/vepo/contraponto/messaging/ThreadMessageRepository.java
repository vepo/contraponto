package dev.vepo.contraponto.messaging;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ThreadMessageRepository {

    private final EntityManager entityManager;

    @Inject
    public ThreadMessageRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<ThreadMessage> findByThreadId(long threadId) {
        return entityManager.createQuery("""
                                         SELECT m
                                         FROM ThreadMessage m
                                         WHERE m.thread.id = :threadId
                                         ORDER BY m.createdAt ASC
                                         """, ThreadMessage.class)
                            .setParameter("threadId", threadId)
                            .getResultList();
    }

    public Optional<ThreadMessage> findLatestByThreadId(long threadId) {
        return entityManager.createQuery("""
                                         SELECT m
                                         FROM ThreadMessage m
                                         WHERE m.thread.id = :threadId
                                         ORDER BY m.createdAt DESC
                                         """, ThreadMessage.class)
                            .setParameter("threadId", threadId)
                            .setMaxResults(1)
                            .getResultStream()
                            .findFirst();
    }

    @Transactional
    public ThreadMessage save(ThreadMessage message) {
        entityManager.persist(message);
        return message;
    }
}
