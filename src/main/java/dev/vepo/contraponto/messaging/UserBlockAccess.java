package dev.vepo.contraponto.messaging;

import dev.vepo.contraponto.user.LoggedUser;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserBlockAccess {

    public boolean canUnblock(UserBlock block, LoggedUser user) {
        return user != null && user.isAuthenticated() && block.getBlocker().getId().equals(user.getId());
    }
}
