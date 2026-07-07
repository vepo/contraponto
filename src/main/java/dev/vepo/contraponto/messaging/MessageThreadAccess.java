package dev.vepo.contraponto.messaging;

import dev.vepo.contraponto.user.LoggedUser;
import dev.vepo.contraponto.user.Role;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MessageThreadAccess {

    public boolean canParticipate(MessageThread thread, LoggedUser user) {
        return user != null && user.isAuthenticated() && thread.isParticipant(user.getId());
    }

    public boolean canReviewReports(LoggedUser user) {
        return user != null && user.hasRole(Role.ADMIN);
    }

    public boolean canViewThread(MessageThread thread, LoggedUser user) {
        return canParticipate(thread, user) || canReviewReports(user);
    }
}
