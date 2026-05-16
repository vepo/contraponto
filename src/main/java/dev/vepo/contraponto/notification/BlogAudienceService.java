package dev.vepo.contraponto.notification;

import java.util.Optional;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.user.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class BlogAudienceService {

    private final BlogAudienceRepository audienceRepository;
    private final BlogRepository blogRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Inject
    public BlogAudienceService(BlogAudienceRepository audienceRepository,
                               BlogRepository blogRepository,
                               UserRepository userRepository,
                               NotificationService notificationService) {
        this.audienceRepository = audienceRepository;
        this.blogRepository = blogRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    public Optional<BlogAudience> findByUserAndBlog(long userId, long blogId) {
        return audienceRepository.findByUserAndBlog(userId, blogId);
    }

    public boolean isEmailSubscribed(long userId, long blogId) {
        return audienceRepository.findByUserAndBlog(userId, blogId).map(BlogAudience::isEmailSubscribed).orElse(false);
    }

    public boolean isFollowing(long userId, long blogId) {
        return audienceRepository.findByUserAndBlog(userId, blogId).map(BlogAudience::isFollowed).orElse(false);
    }

    private void persistOrDelete(BlogAudience audience) {
        if (audience.isActive()) {
            audienceRepository.save(audience);
        } else if (audience.getId() != null) {
            audienceRepository.delete(audience);
        }
    }

    private void rejectOwnBlog(long userId, Blog blog) {
        if (blog.getOwner().getId().equals(userId)) {
            throw new BadRequestException("Cannot follow or subscribe to your own blog");
        }
    }

    private Blog requireActiveBlog(long blogId) {
        Blog blog = blogRepository.findById(blogId).orElseThrow(NotFoundException::new);
        if (!blog.isActive()) {
            throw new NotFoundException("Blog not found");
        }
        return blog;
    }

    @Transactional
    public boolean toggleEmailSubscribe(long userId, long blogId) {
        Blog blog = requireActiveBlog(blogId);
        rejectOwnBlog(userId, blog);
        User user = userRepository.findById(userId).orElseThrow(NotFoundException::new);

        BlogAudience audience = audienceRepository.findByUserAndBlog(userId, blogId)
                                                  .orElseGet(() -> new BlogAudience(user, blog, false, false));
        boolean wasSubscribed = audience.isEmailSubscribed();
        audience.setEmailSubscribed(!wasSubscribed);
        persistOrDelete(audience);

        if (!wasSubscribed && audience.isEmailSubscribed()) {
            notificationService.notifyNewSubscribe(blog.getOwner(), blog, user);
        }
        return audience.isEmailSubscribed();
    }

    @Transactional
    public boolean toggleFollow(long userId, long blogId) {
        Blog blog = requireActiveBlog(blogId);
        rejectOwnBlog(userId, blog);
        User user = userRepository.findById(userId).orElseThrow(NotFoundException::new);

        BlogAudience audience = audienceRepository.findByUserAndBlog(userId, blogId)
                                                  .orElseGet(() -> new BlogAudience(user, blog, false, false));
        boolean wasFollowed = audience.isFollowed();
        audience.setFollowed(!wasFollowed);
        persistOrDelete(audience);

        if (!wasFollowed && audience.isFollowed()) {
            notificationService.notifyNewFollow(blog.getOwner(), blog, user);
        }
        return audience.isFollowed();
    }
}
