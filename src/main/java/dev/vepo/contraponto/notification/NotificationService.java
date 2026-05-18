package dev.vepo.contraponto.notification;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.comment.PostComment;
import dev.vepo.contraponto.git.GitSyncRun;
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
                        PostComment comment,
                        User actor) {
        create(recipient, type, blog, post, publication, comment, actor, null);
    }

    private void create(User recipient,
                        NotificationType type,
                        Blog blog,
                        Post post,
                        PostPublication publication,
                        PostComment comment,
                        User actor,
                        GitSyncRun gitSyncRun) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(type);
        notification.setBlog(blog);
        notification.setPost(post);
        notification.setPublication(publication);
        notification.setComment(comment);
        notification.setActor(actor);
        notification.setGitSyncRun(gitSyncRun);
        notification.setRead(false);
        notificationRepository.create(notification);
    }

    @Transactional
    public void notifyGitSyncFailed(User recipient, Blog blog, GitSyncRun run) {
        create(recipient, NotificationType.GIT_SYNC_FAILED, blog, run.getPost(), null, null, null, run);
    }

    @Transactional
    public void notifyGitSyncSucceeded(User recipient, Blog blog, GitSyncRun run) {
        create(recipient, NotificationType.GIT_SYNC_SUCCEEDED, blog, run.getPost(), null, null, null, run);
    }

    @Transactional
    public void notifyNewComment(User recipient, Post post, PostComment comment, User actor) {
        create(recipient, NotificationType.NEW_COMMENT, post.getBlog(), post, null, comment, actor);
    }

    @Transactional
    public void notifyNewFollow(User recipient, Blog blog, User actor) {
        create(recipient, NotificationType.NEW_FOLLOW, blog, null, null, null, actor);
    }

    @Transactional
    public void notifyNewPost(User recipient, Blog blog, Post post, PostPublication publication) {
        create(recipient, NotificationType.NEW_POST, blog, post, publication, null, null);
    }

    @Transactional
    public void notifyNewSubscribe(User recipient, Blog blog, User actor) {
        create(recipient, NotificationType.NEW_SUBSCRIBE, blog, null, null, null, actor);
    }
}
