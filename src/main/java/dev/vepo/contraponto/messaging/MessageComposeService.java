package dev.vepo.contraponto.messaging;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;

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
public class MessageComposeService {

    public static final int MAX_TITLE_LENGTH = 200;
    public static final int MAX_BODY_LENGTH = 2000;
    public static final int MIN_RECIPIENT_SUGGESTION_LENGTH = 2;
    public static final int MAX_RECIPIENT_SUGGESTIONS = 10;

    private final MessageThreadRepository threadRepository;
    private final ThreadMessageRepository messageRepository;
    private final MessageThreadParticipantRepository participantRepository;
    private final UserBlockRepository blockRepository;
    private final UserRepository userRepository;
    private final Event<MessageThreadCreatedEvent> threadCreatedEvent;
    private final int maxThreadsPerDay;

    @Inject
    public MessageComposeService(MessageThreadRepository threadRepository,
                                 ThreadMessageRepository messageRepository,
                                 MessageThreadParticipantRepository participantRepository,
                                 UserBlockRepository blockRepository,
                                 UserRepository userRepository,
                                 Event<MessageThreadCreatedEvent> threadCreatedEvent,
                                 @ConfigProperty(name = "app.messaging.compose.max-threads-per-day", defaultValue = "10") int maxThreadsPerDay) {
        this.threadRepository = threadRepository;
        this.messageRepository = messageRepository;
        this.participantRepository = participantRepository;
        this.blockRepository = blockRepository;
        this.userRepository = userRepository;
        this.threadCreatedEvent = threadCreatedEvent;
        this.maxThreadsPerDay = maxThreadsPerDay;
    }

    @Transactional
    public MessageThread compose(long initiatorUserId, String recipientUsername, String title, String body) {
        User initiator = userRepository.findById(initiatorUserId).orElseThrow(NotFoundException::new);
        User recipient = userRepository.findActiveByUsername(recipientUsername.trim())
                                       .orElseThrow(() -> new NotFoundException("User not found."));
        if (recipient.getId().equals(initiatorUserId)) {
            throw new BadRequestException("You cannot message yourself.");
        }
        if (blockRepository.isBlockedEitherDirection(initiatorUserId, recipient.getId())) {
            throw new ForbiddenException("You cannot message this user.");
        }
        enforceRateLimit(initiatorUserId);

        String trimmedTitle = validateTitle(title);
        String trimmedBody = validateBody(body);

        MessageThread thread = new MessageThread();
        thread.setInitiator(initiator);
        thread.setRecipient(recipient);
        thread.setTitle(trimmedTitle);
        threadRepository.save(thread);

        ThreadMessage message = new ThreadMessage();
        message.setThread(thread);
        message.setAuthor(initiator);
        message.setBody(trimmedBody);
        messageRepository.save(message);

        createParticipant(thread, initiator, message);
        createParticipant(thread, recipient, null);

        threadCreatedEvent.fire(new MessageThreadCreatedEvent(thread.getId(), initiatorUserId, recipient.getId()));
        return thread;
    }

    private void createParticipant(MessageThread thread, User user, ThreadMessage readUpTo) {
        MessageThreadParticipant participant = new MessageThreadParticipant();
        participant.setThread(thread);
        participant.setUser(user);
        if (readUpTo != null) {
            participant.markRead(readUpTo);
        }
        participantRepository.save(participant);
    }

    private void enforceRateLimit(long initiatorUserId) {
        var since = LocalDateTime.now(ZoneId.systemDefault()).minusHours(24);
        long count = threadRepository.countNewThreadsSince(initiatorUserId, since);
        if (count >= maxThreadsPerDay) {
            throw new BadRequestException("You have reached the limit of %s new threads per 24 hours.".formatted(maxThreadsPerDay));
        }
    }

    public List<RecipientSuggestion> suggestRecipients(long initiatorUserId, String usernamePrefix) {
        if (usernamePrefix == null || usernamePrefix.trim().length() < MIN_RECIPIENT_SUGGESTION_LENGTH) {
            return List.of();
        }
        return userRepository.findActiveByUsernamePrefixExcluding(initiatorUserId,
                                                                  usernamePrefix,
                                                                  MAX_RECIPIENT_SUGGESTIONS)
                             .stream()
                             .filter(candidate -> !blockRepository.isBlockedEitherDirection(initiatorUserId,
                                                                                            candidate.getId()))
                             .map(candidate -> new RecipientSuggestion(candidate.getUsername(), candidate.getName()))
                             .toList();
    }

    private String validateBody(String body) {
        if (body == null) {
            throw new BadRequestException("Message body is required.");
        }
        String trimmed = body.trim();
        if (trimmed.isEmpty()) {
            throw new BadRequestException("Message body is required.");
        }
        if (trimmed.length() > MAX_BODY_LENGTH) {
            throw new BadRequestException("Message must be at most %s characters.".formatted(MAX_BODY_LENGTH));
        }
        return trimmed;
    }

    private String validateTitle(String title) {
        if (title == null) {
            throw new BadRequestException("Thread title is required.");
        }
        String trimmed = title.trim();
        if (trimmed.isEmpty()) {
            throw new BadRequestException("Thread title is required.");
        }
        if (trimmed.length() > MAX_TITLE_LENGTH) {
            throw new BadRequestException("Thread title must be at most %s characters.".formatted(MAX_TITLE_LENGTH));
        }
        return trimmed;
    }
}
