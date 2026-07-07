package dev.vepo.contraponto.notification;

import dev.vepo.contraponto.messaging.MessageThread;
import dev.vepo.contraponto.messaging.MessageThreadCreatedEvent;
import dev.vepo.contraponto.messaging.MessageThreadRepository;
import dev.vepo.contraponto.messaging.ThreadMessagePostedEvent;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.user.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class MessageNotificationObserver {

    private final MessageThreadRepository threadRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @Inject
    public MessageNotificationObserver(MessageThreadRepository threadRepository,
                                       NotificationService notificationService,
                                       UserRepository userRepository) {
        this.threadRepository = threadRepository;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @Transactional
    void onThreadCreated(@Observes MessageThreadCreatedEvent event) {
        MessageThread thread = threadRepository.findById(event.threadId()).orElse(null);
        User initiator = userRepository.findById(event.initiatorUserId()).orElse(null);
        User recipient = userRepository.findById(event.recipientUserId()).orElse(null);
        if (thread == null || initiator == null || recipient == null) {
            return;
        }
        notificationService.notifyNewMessageThread(recipient, thread, initiator);
    }

    @Transactional
    void onThreadMessagePosted(@Observes ThreadMessagePostedEvent event) {
        MessageThread thread = threadRepository.findById(event.threadId()).orElse(null);
        User author = userRepository.findById(event.authorUserId()).orElse(null);
        User recipient = userRepository.findById(event.recipientUserId()).orElse(null);
        if (thread == null || author == null || recipient == null) {
            return;
        }
        notificationService.notifyNewThreadMessage(recipient, thread, author);
    }
}
