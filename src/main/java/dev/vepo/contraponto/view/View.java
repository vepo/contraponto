package dev.vepo.contraponto.view;

import java.time.LocalDateTime;

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
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_views")
public class View {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // null if anonymous

    @Column(name = "session_id", nullable = false)
    private String sessionId; // unique per browser session (for anonymous)

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;

    // Required by JPA
    public View() {}

    public View(Post post, User user, String sessionId, LocalDateTime viewedAt) {
        this.post = post;
        this.user = user;
        this.sessionId = sessionId;
        this.viewedAt = viewedAt;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public Post getPost() {
        return post;
    }

    public String getSessionId() {
        return sessionId;
    }

    public User getUser() {
        return user;
    }

    public LocalDateTime getViewedAt() {
        return viewedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setPost(Post post) {
        this.post = post;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setViewedAt(LocalDateTime viewedAt) {
        this.viewedAt = viewedAt;
    }

    @Override
    public String toString() {
        return "View[id=%d]".formatted(id);
    }
}