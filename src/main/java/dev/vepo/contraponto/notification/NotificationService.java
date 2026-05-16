package dev.vepo.contraponto.notification;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostPublication;
import dev.vepo.contraponto.user.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Inject
    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    private void create(User recipient,
                        NotificationType type,
                        Blog blog,
                        Post post,
                        PostPublication publication,
                        User actor) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(type);
        notification.setBlog(blog);
        notification.setPost(post);
        notification.setPublication(publication);
        notification.setActor(actor);
        notification.setRead(false);
        notificationRepository.create(notification);
    }

    @Transactional
    public void notifyNewFollow(User recipient, Blog blog, User actor) {
        create(recipient, NotificationType.NEW_FOLLOW, blog, null, null, actor);
    }

    @Transactional
    public void notifyNewPost(User recipient, Blog blog, Post post, PostPublication publication) {
        create(recipient, NotificationType.NEW_POST, blog, post, publication, null);
    }

    @Transactional
    public void notifyNewSubscribe(User recipient, Blog blog, User actor) {
        create(recipient, NotificationType.NEW_SUBSCRIBE, blog, null, null, actor);
    }
}
