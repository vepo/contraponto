package dev.vepo.contraponto.reddit.delivery;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

import dev.vepo.contraponto.post.Post;
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
@Table(name = "tb_reddit_deliveries")
public class RedditDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RedditDeliveryKind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RedditDeliveryStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    public RedditDelivery() {}

    public RedditDelivery(Post post, User user, RedditDeliveryKind kind) {
        this.post = post;
        this.user = user;
        this.kind = kind;
        this.status = RedditDeliveryStatus.PENDING;
        this.attempts = 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        RedditDelivery other = (RedditDelivery) obj;
        if (id == null || other.id == null) {
            return false;
        }
        return Objects.equals(id, other.id);
    }

    public int getAttempts() {
        return attempts;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public Long getId() {
        return id;
    }

    public RedditDeliveryKind getKind() {
        return kind;
    }

    public String getLastError() {
        return lastError;
    }

    public LocalDateTime getNextRetryAt() {
        return nextRetryAt;
    }

    public Post getPost() {
        return post;
    }

    public RedditDeliveryStatus getStatus() {
        return status;
    }

    public User getUser() {
        return user;
    }

    @Override
    public int hashCode() {
        return id == null ? System.identityHashCode(this) : Objects.hash(id);
    }

    public boolean isExhausted() {
        return status == RedditDeliveryStatus.FAILED;
    }

    public void markDelivered() {
        this.status = RedditDeliveryStatus.DELIVERED;
        this.deliveredAt = LocalDateTime.now(ZoneId.systemDefault());
        this.lastError = null;
    }

    public void markFailed(String error, LocalDateTime nextRetryAt) {
        this.attempts = attempts + 1;
        this.lastError = (error == null || error.isBlank()) ? "unknown delivery failure" : error;
        this.nextRetryAt = nextRetryAt;
        if (attempts >= 5) {
            this.status = RedditDeliveryStatus.FAILED;
        } else {
            this.status = RedditDeliveryStatus.PENDING;
        }
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now(ZoneId.systemDefault());
        }
        if (status == null) {
            status = RedditDeliveryStatus.PENDING;
        }
    }
}
