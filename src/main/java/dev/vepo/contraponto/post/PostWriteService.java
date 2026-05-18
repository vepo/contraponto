package dev.vepo.contraponto.post;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.blog.BlogAccess;
import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

/**
 * Resolves posts for draft/publish mutations with ownership checks to prevent
 * IDOR.
 */
@ApplicationScoped
public class PostWriteService {

    private final PostRepository postRepository;
    private final BlogRepository blogRepository;
    private final BlogAccess blogAccess;

    @Inject
    public PostWriteService(PostRepository postRepository, BlogRepository blogRepository, BlogAccess blogAccess) {
        this.postRepository = postRepository;
        this.blogRepository = blogRepository;
        this.blogAccess = blogAccess;
    }

    public Blog requireEditableBlog(long blogId, LoggedUser user) {
        return blogRepository.findById(blogId)
                             .filter(b -> blogAccess.canEdit(b, user))
                             .orElseThrow(() -> new NotFoundException("Blog not found! blogId=%s".formatted(blogId)));
    }

    public Post resolvePostForWrite(Long postId, Blog blog, LoggedUser user) {
        if (postId == null) {
            var post = new Post();
            post.setBlog(blog);
            return post;
        }
        var post = postRepository.findByIdWithTags(postId)
                                 .orElseThrow(() -> new NotFoundException("Post not found! id=%s".formatted(postId)));
        if (!blogAccess.canEdit(post.getBlog(), user)) {
            throw new NotFoundException("Post not found! id=%s".formatted(postId));
        }
        if (!post.getBlog().getId().equals(blog.getId())) {
            throw new NotFoundException("Post not found! id=%s".formatted(postId));
        }
        return post;
    }
}
