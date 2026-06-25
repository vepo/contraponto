package dev.vepo.contraponto.user;

import java.util.Optional;
import java.util.UUID;

import dev.vepo.contraponto.shared.security.SessionStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class LoggedUserProvider {

    private final SessionStore sessionStore;
    private final UserRepository userRepository;

    @Inject
    public LoggedUserProvider(SessionStore sessionStore, UserRepository userRepository) {
        this.sessionStore = sessionStore;
        this.userRepository = userRepository;
    }

    public Optional<LoggedUser> find(String sessionId) {
        return sessionStore.findUserId(sessionId)
                           .flatMap(userRepository::findByIdForSession)
                           .flatMap(user -> {
                               if (!user.isActive()) {
                                   sessionStore.remove(sessionId);
                                   return Optional.empty();
                               }
                               return Optional.of(new LoggedUser(user, sessionId));
                           });
    }

    public void invalidateAllSessionsForUser(long userId) {
        sessionStore.removeAllForUser(userId);
    }

    public LoggedUser login(User user) {
        var sessionId = UUID.randomUUID().toString();
        sessionStore.put(sessionId, user.getId());
        return new LoggedUser(user, sessionId);
    }

    public void logout(LoggedUser user) {
        sessionStore.remove(user.getSessionId());
    }

    public void update(String sessionId, User user) {
        sessionStore.put(sessionId, user.getId());
    }
}
