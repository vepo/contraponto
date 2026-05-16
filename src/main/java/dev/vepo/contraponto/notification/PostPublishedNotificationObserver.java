package dev.vepo.contraponto.notification;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostPublication;
import dev.vepo.contraponto.post.PostPublicationRepository;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.user.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class PostPublishedNotificationObserver {

    private final BlogAudienceRepository audienceRepository;
    private final NotificationService notificationService;
    private final PostNotificationEmailService emailService;
    private final PostRepository postRepository;
    private final PostPublicationRepository publicationRepository;

    @Inject
    public PostPublishedNotificationObserver(BlogAudienceRepository audienceRepository,
                                             NotificationService notificationService,
                                             PostNotificationEmailService emailService,
                                             PostRepository postRepository,
                                             PostPublicationRepository publicationRepository) {
        this.audienceRepository = audienceRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.postRepository = postRepository;
        this.publicationRepository = publicationRepository;
    }

    @Transactional
    void afterPublish(@Observes PostPublishedEvent event) {
        Post post = postRepository.findById(event.postId()).orElse(null);
        PostPublication publication = publicationRepository.findById(event.publicationId()).orElse(null);
        if (post == null || publication == null) {
            return;
        }

        Blog blog = post.getBlog();
        long authorId = event.authorUserId();

        for (BlogAudience audience : audienceRepository.findFollowersByBlogId(event.blogId())) {
            User follower = audience.getUser();
            if (follower.getId().equals(authorId)) {
                continue;
            }
            notificationService.notifyNewPost(follower, blog, post, publication);
        }

        for (BlogAudience audience : audienceRepository.findEmailSubscribersByBlogId(event.blogId())) {
            User subscriber = audience.getUser();
            if (subscriber.getId().equals(authorId)) {
                continue;
            }
            emailService.sendIfNotSent(subscriber, post, publication, blog);
        }
    }
}
