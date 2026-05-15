package dev.vepo.contraponto.custompage;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CustomPageAccess {

    public boolean canEdit(CustomPage page, LoggedUser user) {
        if (!user.isAuthenticated()) {
            return false;
        }
        if (user.isEditor()) {
            return true;
        }
        if (page.getBlog() == null) {
            return false;
        }
        return page.getBlog().getOwner().getId().equals(user.getId());
    }

    public boolean canEditBlog(Blog blog, LoggedUser user) {
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

    public boolean canManageApplicationPages(LoggedUser user) {
        return user.isEditor();
    }
}
