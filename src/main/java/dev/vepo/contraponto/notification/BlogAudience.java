package dev.vepo.contraponto.notification;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

import org.hibernate.annotations.UpdateTimestamp;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "tb_blog_audience", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "blog_id" }))
public class BlogAudience {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blog_id", nullable = false)
    private Blog blog;

    @Column(nullable = false)
    private boolean followed;

    @Column(name = "email_subscribed", nullable = false)
    private boolean emailSubscribed;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public BlogAudience() {}

    public BlogAudience(User user, Blog blog, boolean followed, boolean emailSubscribed) {
        this.user = user;
        this.blog = blog;
        this.followed = followed;
        this.emailSubscribed = emailSubscribed;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        BlogAudience other = (BlogAudience) obj;
        if (id == null || other.id == null) {
            return false;
        }
        return Objects.equals(other.id, id);
    }

    public Blog getBlog() {
        return blog;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public User getUser() {
        return user;
    }

    @Override
    public int hashCode() {
        return id == null ? System.identityHashCode(this) : Objects.hash(id);
    }

    public boolean isActive() {
        return followed || emailSubscribed;
    }

    public boolean isEmailSubscribed() {
        return emailSubscribed;
    }

    public boolean isFollowed() {
        return followed;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now(ZoneId.systemDefault());
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now(ZoneId.systemDefault());
    }

    public void setBlog(Blog blog) {
        this.blog = blog;
    }

    public void setEmailSubscribed(boolean emailSubscribed) {
        this.emailSubscribed = emailSubscribed;
    }

    public void setFollowed(boolean followed) {
        this.followed = followed;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
