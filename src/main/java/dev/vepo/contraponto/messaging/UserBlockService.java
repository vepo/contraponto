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

    private final UserBlockRepository blockRepository;
    private final UserRepository userRepository;
    private final Event<UserBlockedEvent> userBlockedEvent;
    private final Event<UserUnblockedEvent> userUnblockedEvent;

    @Inject
    public UserBlockService(UserBlockRepository blockRepository,
                            UserRepository userRepository,
                            Event<UserBlockedEvent> userBlockedEvent,
                            Event<UserUnblockedEvent> userUnblockedEvent) {
        this.blockRepository = blockRepository;
        this.userRepository = userRepository;
        this.userBlockedEvent = userBlockedEvent;
        this.userUnblockedEvent = userUnblockedEvent;
    }

    @Transactional
    public UserBlock block(long blockerUserId, long blockedUserId) {
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
        blockRepository.save(block);

        userBlockedEvent.fire(new UserBlockedEvent(blockerUserId, blockedUserId));
        return block;
    }

    @Transactional
    public void blockByUsername(long blockerUserId, String blockedUsername) {
        User blocked = userRepository.findPublicAuthorByUsername(blockedUsername.trim())
                                     .orElseThrow(() -> new NotFoundException("User not found."));
        block(blockerUserId, blocked.getId());
    }

    @Transactional
    public void unblock(long blockerUserId, long blockedUserId) {
        UserBlock block = blockRepository.findByBlockerAndBlocked(blockerUserId, blockedUserId)
                                         .orElseThrow(NotFoundException::new);
        blockRepository.delete(block);
        userUnblockedEvent.fire(new UserUnblockedEvent(blockerUserId, blockedUserId));
    }
}
