package dev.vepo.contraponto.messaging;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class MessageThreadThawObserver {

    private final MessageThreadRepository threadRepository;

    @Inject
    public MessageThreadThawObserver(MessageThreadRepository threadRepository) {
        this.threadRepository = threadRepository;
    }

    @Transactional
    void onUserUnblocked(@Observes UserUnblockedEvent event) {
        for (MessageThread thread : threadRepository.findFrozenBetweenUsers(event.blockerUserId(), event.blockedUserId())) {
            thread.thaw();
            threadRepository.save(thread);
        }
    }
}
