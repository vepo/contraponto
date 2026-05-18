package dev.vepo.contraponto.blog;

import dev.vepo.contraponto.shared.infra.LoggedUser;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BlogAccess {

    public boolean canCreate(LoggedUser user) {
        return user.isAuthenticated();
    }

    public boolean canDeactivate(Blog blog, LoggedUser user) {
        if (!user.isAuthenticated() || blog.isMain()) {
            return false;
        }
        if (blog.getOwner().getId().equals(user.getId())) {
            return true;
        }
        return user.isEditor();
    }

    public boolean canDelete(Blog blog, LoggedUser user) {
        return canDeactivate(blog, user);
    }

    public boolean canEdit(Blog blog, LoggedUser user) {
        if (!user.isAuthenticated()) {
            return false;
        }
        return blog.getOwner().getId().equals(user.getId());
    }

    public boolean canListAll(LoggedUser user) {
        return user.isEditor();
    }
}
