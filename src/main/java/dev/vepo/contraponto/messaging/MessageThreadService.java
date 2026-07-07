package dev.vepo.contraponto.messaging;

import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.user.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class MessageThreadService {

    private final MessageThreadRepository threadRepository;
    private final ThreadMessageRepository messageRepository;
    private final MessageThreadParticipantRepository participantRepository;
    private final MessageReportRepository reportRepository;
    private final UserBlockRepository blockRepository;
    private final UserRepository userRepository;
    private final Event<ThreadMessagePostedEvent> messagePostedEvent;
    private final Event<MessageThreadClosedEvent> threadClosedEvent;
    private final Event<MessageThreadFlaggedEvent> threadFlaggedEvent;

    @Inject
    public MessageThreadService(MessageThreadRepository threadRepository,
                                ThreadMessageRepository messageRepository,
                                MessageThreadParticipantRepository participantRepository,
                                MessageReportRepository reportRepository,
                                UserBlockRepository blockRepository,
                                UserRepository userRepository,
                                Event<ThreadMessagePostedEvent> messagePostedEvent,
                                Event<MessageThreadClosedEvent> threadClosedEvent,
                                Event<MessageThreadFlaggedEvent> threadFlaggedEvent) {
        this.threadRepository = threadRepository;
        this.messageRepository = messageRepository;
        this.participantRepository = participantRepository;
        this.reportRepository = reportRepository;
        this.blockRepository = blockRepository;
        this.userRepository = userRepository;
        this.messagePostedEvent = messagePostedEvent;
        this.threadClosedEvent = threadClosedEvent;
        this.threadFlaggedEvent = threadFlaggedEvent;
    }

    @Transactional
    public void close(long threadId, long userId) {
        MessageThread thread = requireParticipantThread(threadId, userId);
        if (!thread.getStatus().acceptsReplies()) {
            throw new BadRequestException("This thread is already closed.");
        }
        thread.close();
        threadRepository.save(thread);
        threadClosedEvent.fire(new MessageThreadClosedEvent(threadId, userId));
    }

    @Transactional
    public MessageReport flag(long threadId, long reporterUserId) {
        MessageThread thread = requireParticipantThread(threadId, reporterUserId);
        return reportRepository.findByThreadAndReporter(threadId, reporterUserId)
                               .orElseGet(() -> {
                                   User reporter = userRepository.findById(reporterUserId).orElseThrow(NotFoundException::new);
                                   MessageReport report = new MessageReport();
                                   report.setThread(thread);
                                   report.setReporter(reporter);
                                   reportRepository.save(report);
                                   threadFlaggedEvent.fire(new MessageThreadFlaggedEvent(threadId, report.getId(), reporterUserId));
                                   return report;
                               });
    }

    public MessageThreadView loadThreadView(long threadId, long userId) {
        MessageThread thread = requireParticipantThread(threadId, userId);
        var messages = messageRepository.findByThreadId(threadId);
        User other = thread.getOtherParticipant(userId);
        boolean blockedEither = blockRepository.isBlockedEitherDirection(thread.getInitiator().getId(),
                                                                         thread.getRecipient().getId());
        boolean blockedByCurrentUser = blockRepository.findByBlockerAndBlocked(userId, other.getId()).isPresent();
        boolean showBlockedBanner = blockedEither || thread.getStatus() == MessageThreadStatus.FROZEN;
        markRead(threadId, userId, messages);
        return new MessageThreadView(thread, messages, other, showBlockedBanner, blockedByCurrentUser);
    }

    private void markRead(long threadId, long userId, java.util.List<ThreadMessage> messages) {
        if (messages.isEmpty()) {
            return;
        }
        markReadForUser(threadId, userId, messages.get(messages.size() - 1));
    }

    private void markReadForUser(long threadId, long userId, ThreadMessage message) {
        participantRepository.findByThreadAndUser(threadId, userId).ifPresent(participant -> {
            participant.markRead(message);
            participantRepository.save(participant);
        });
    }

    @Transactional
    public ThreadMessage reply(long threadId, long authorUserId, String body) {
        MessageThread thread = requireParticipantThread(threadId, authorUserId);
        if (!thread.getStatus().acceptsReplies()) {
            throw new BadRequestException("This thread no longer accepts replies.");
        }
        if (blockRepository.isBlockedEitherDirection(thread.getInitiator().getId(), thread.getRecipient().getId())) {
            throw new ForbiddenException("You cannot reply on this thread.");
        }

        String trimmed = validateBody(body);
        User author = userRepository.findById(authorUserId).orElseThrow(NotFoundException::new);
        ThreadMessage message = new ThreadMessage();
        message.setThread(thread);
        message.setAuthor(author);
        message.setBody(trimmed);
        messageRepository.save(message);

        long recipientUserId = thread.getOtherParticipant(authorUserId).getId();
        markReadForUser(threadId, authorUserId, message);
        messagePostedEvent.fire(new ThreadMessagePostedEvent(threadId, message.getId(), authorUserId, recipientUserId));
        return message;
    }

    private MessageThread requireParticipantThread(long threadId, long userId) {
        return threadRepository.findByIdForParticipant(threadId, userId).orElseThrow(NotFoundException::new);
    }

    private String validateBody(String body) {
        if (body == null) {
            throw new BadRequestException("Message body is required.");
        }
        String trimmed = body.trim();
        if (trimmed.isEmpty()) {
            throw new BadRequestException("Message body is required.");
        }
        if (trimmed.length() > MessageComposeService.MAX_BODY_LENGTH) {
            throw new BadRequestException("Message must be at most %s characters.".formatted(MessageComposeService.MAX_BODY_LENGTH));
        }
        return trimmed;
    }
}
