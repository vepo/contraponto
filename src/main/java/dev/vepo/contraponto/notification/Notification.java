package dev.vepo.contraponto.notification;

import java.time.LocalDateTime;
import java.util.Objects;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.comment.PostComment;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostPublication;
import dev.vepo.contraponto.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blog_id", nullable = false)
    private Blog blog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publication_id")
    private PostPublication publication;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private PostComment comment;

    @Column(nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Required by JPA
    public Notification() {}

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Notification other = (Notification) obj;
        if (id == null || other.id == null) {
            return false;
        }
        return Objects.equals(other.id, id);
    }

    public User getActor() {
        return actor;
    }

    public Blog getBlog() {
        return blog;
    }

    public PostComment getComment() {
        return comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getId() {
        return id;
    }

    public Post getPost() {
        return post;
    }

    public PostPublication getPublication() {
        return publication;
    }

    public User getRecipient() {
        return recipient;
    }

    public NotificationType getType() {
        return type;
    }

    @Override
    public int hashCode() {
        return id == null ? System.identityHashCode(this) : Objects.hash(id);
    }

    public boolean isRead() {
        return read;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void setActor(User actor) {
        this.actor = actor;
    }

    public void setBlog(Blog blog) {
        this.blog = blog;
    }

    public void setComment(PostComment comment) {
        this.comment = comment;
    }

    public void setPost(Post post) {
        this.post = post;
    }

    public void setPublication(PostPublication publication) {
        this.publication = publication;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public void setRecipient(User recipient) {
        this.recipient = recipient;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }
}
