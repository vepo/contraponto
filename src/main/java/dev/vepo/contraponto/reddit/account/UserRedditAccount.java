package dev.vepo.contraponto.reddit.account;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

import dev.vepo.contraponto.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_user_reddit_accounts")
public class UserRedditAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "reddit_username", nullable = false)
    private String redditUsername;

    @Column(name = "encrypted_refresh_token", nullable = false, columnDefinition = "TEXT")
    private String encryptedRefreshToken;

    @Column(name = "linked_at", nullable = false)
    private LocalDateTime linkedAt;

    public UserRedditAccount() {}

    public UserRedditAccount(User user, String redditUsername, String encryptedRefreshToken) {
        this.user = user;
        this.redditUsername = redditUsername;
        this.encryptedRefreshToken = encryptedRefreshToken;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        UserRedditAccount other = (UserRedditAccount) obj;
        return Objects.equals(id, other.id);
    }

    public String getEncryptedRefreshToken() {
        return encryptedRefreshToken;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getLinkedAt() {
        return linkedAt;
    }

    public String getRedditUsername() {
        return redditUsername;
    }

    public User getUser() {
        return user;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @PrePersist
    void onCreate() {
        if (linkedAt == null) {
            linkedAt = LocalDateTime.now(ZoneId.systemDefault());
        }
    }

    public void setEncryptedRefreshToken(String encryptedRefreshToken) {
        this.encryptedRefreshToken = encryptedRefreshToken;
    }

    public void setRedditUsername(String redditUsername) {
        this.redditUsername = redditUsername;
    }
}
