package dev.vepo.contraponto.messaging;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class MessageThreadFreezeObserver {

    private final MessageThreadRepository threadRepository;

    @Inject
    public MessageThreadFreezeObserver(MessageThreadRepository threadRepository) {
        this.threadRepository = threadRepository;
    }

    @Transactional
    void onUserBlocked(@Observes UserBlockedEvent event) {
        for (MessageThread thread : threadRepository.findOpenBetweenUsers(event.blockerUserId(), event.blockedUserId())) {
            thread.freeze();
            threadRepository.save(thread);
        }
    }
}
