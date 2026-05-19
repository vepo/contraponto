package dev.vepo.contraponto.readingtime;

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
@Table(name = "tb_reading_sessions")
public class ReadingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "last_activity_at", nullable = false)
    private LocalDateTime lastActivityAt;

    @Column(name = "total_seconds", nullable = false)
    private int totalSeconds;

    public ReadingSession() {}

    public ReadingSession(Post post, User user, String sessionId, LocalDateTime startedAt) {
        this.post = post;
        this.user = user;
        this.sessionId = sessionId;
        this.startedAt = startedAt;
        this.lastActivityAt = startedAt;
        this.totalSeconds = 0;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getLastActivityAt() {
        return lastActivityAt;
    }

    public Post getPost() {
        return post;
    }

    public String getSessionId() {
        return sessionId;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public int getTotalSeconds() {
        return totalSeconds;
    }

    public User getUser() {
        return user;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setLastActivityAt(LocalDateTime lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public void setPost(Post post) {
        this.post = post;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public void setTotalSeconds(int totalSeconds) {
        this.totalSeconds = totalSeconds;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
