package dev.vepo.contraponto.post;

import dev.vepo.contraponto.blog.BlogAccess;
import dev.vepo.contraponto.user.LoggedUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PostAccess {

    private final BlogAccess blogAccess;

    @Inject
    public PostAccess(BlogAccess blogAccess) {
        this.blogAccess = blogAccess;
    }

    public boolean canManage(Post post, LoggedUser user) {
        return blogAccess.canEdit(post.getBlog(), user);
    }
}
