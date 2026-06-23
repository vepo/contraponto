package dev.vepo.contraponto.readinglist;

import java.time.LocalDateTime;
import java.time.ZoneId;

import dev.vepo.contraponto.post.Post;
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
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_reading_list_items")
public class ReadingListItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "saved_at", nullable = false)
    private LocalDateTime savedAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public ReadingListItem() {}

    public ReadingListItem(User user, Post post) {
        this.user = user;
        this.post = post;
    }

    public void clearReadMark() {
        readAt = null;
    }

    public Long getId() {
        return id;
    }

    public Post getPost() {
        return post;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public LocalDateTime getSavedAt() {
        return savedAt;
    }

    public User getUser() {
        return user;
    }

    public boolean isUnread() {
        return readAt == null;
    }

    public void markRead() {
        readAt = LocalDateTime.now(ZoneId.systemDefault());
    }

    @PrePersist
    void onCreate() {
        if (savedAt == null) {
            savedAt = LocalDateTime.now(ZoneId.systemDefault());
        }
    }

    public void requeue() {
        readAt = null;
        savedAt = LocalDateTime.now(ZoneId.systemDefault());
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setPost(Post post) {
        this.post = post;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public void setSavedAt(LocalDateTime savedAt) {
        this.savedAt = savedAt;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
