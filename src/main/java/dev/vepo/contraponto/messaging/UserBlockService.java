package dev.vepo.contraponto.messaging;

import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.user.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class UserBlockService {

    public static final int MAX_REASON_LENGTH = 500;

    private final UserBlockRepository blockRepository;
    private final MessageThreadRepository threadRepository;
    private final UserRepository userRepository;
    private final Event<UserBlockedEvent> userBlockedEvent;

    @Inject
    public UserBlockService(UserBlockRepository blockRepository,
                            MessageThreadRepository threadRepository,
                            UserRepository userRepository,
                            Event<UserBlockedEvent> userBlockedEvent) {
        this.blockRepository = blockRepository;
        this.threadRepository = threadRepository;
        this.userRepository = userRepository;
        this.userBlockedEvent = userBlockedEvent;
    }

    @Transactional
    public UserBlock block(long blockerUserId, long blockedUserId, String reason) {
        if (blockerUserId == blockedUserId) {
            throw new BadRequestException("You cannot block yourself.");
        }
        blockRepository.findByBlockerAndBlocked(blockerUserId, blockedUserId).ifPresent(existing -> {
            throw new BadRequestException("User is already blocked.");
        });

        User blocker = userRepository.findById(blockerUserId).orElseThrow(NotFoundException::new);
        User blocked = userRepository.findById(blockedUserId).orElseThrow(NotFoundException::new);

        UserBlock block = new UserBlock();
        block.setBlocker(blocker);
        block.setBlocked(blocked);
        block.setReason(validateReason(reason));
        blockRepository.save(block);

        userBlockedEvent.fire(new UserBlockedEvent(blockerUserId, blockedUserId));
        return block;
    }

    @Transactional
    public void blockByUsername(long blockerUserId, String blockedUsername, String reason) {
        User blocked = userRepository.findPublicAuthorByUsername(blockedUsername.trim())
                                     .orElseThrow(() -> new NotFoundException("User not found."));
        block(blockerUserId, blocked.getId(), reason);
    }

    @Transactional
    public void unblock(long blockerUserId, long blockedUserId) {
        UserBlock block = blockRepository.findByBlockerAndBlocked(blockerUserId, blockedUserId)
                                         .orElseThrow(NotFoundException::new);
        blockRepository.delete(block);
    }

    private String validateReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        String trimmed = reason.trim();
        if (trimmed.length() > MAX_REASON_LENGTH) {
            throw new BadRequestException("Block reason must be at most %s characters.".formatted(MAX_REASON_LENGTH));
        }
        return trimmed;
    }
}
