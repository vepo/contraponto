package dev.vepo.contraponto.messaging;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class MessageThreadParticipantRepository {

    private final EntityManager entityManager;

    @Inject
    public MessageThreadParticipantRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<MessageThreadParticipant> findByThreadAndUser(long threadId, long userId) {
        return entityManager.createQuery("""
                                         SELECT p
                                         FROM MessageThreadParticipant p
                                         WHERE p.thread.id = :threadId AND p.user.id = :userId
                                         """, MessageThreadParticipant.class)
                            .setParameter("threadId", threadId)
                            .setParameter("userId", userId)
                            .getResultStream()
                            .findFirst();
    }

    @Transactional
    public MessageThreadParticipant save(MessageThreadParticipant participant) {
        if (participant.getId() == null) {
            entityManager.persist(participant);
        } else {
            participant = entityManager.merge(participant);
        }
        return participant;
    }
}
