package dev.vepo.contraponto.blog;

import dev.vepo.contraponto.shared.infra.LoggedUser;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BlogAccess {

    public boolean canCreate(LoggedUser user) {
        return user.isAuthenticated();
    }

    public boolean canDelete(Blog blog, LoggedUser user) {
        if (!canEdit(blog, user)) {
            return false;
        }
        return !blog.isMain();
    }

    public boolean canEdit(Blog blog, LoggedUser user) {
        if (!user.isAuthenticated()) {
            return false;
        }
        if (user.isEditor()) {
            return true;
        }
        return blog.getOwner().getId().equals(user.getId());
    }

    public boolean canListAll(LoggedUser user) {
        return user.isEditor();
    }
}
