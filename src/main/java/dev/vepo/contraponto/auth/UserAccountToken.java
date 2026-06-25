package dev.vepo.contraponto.auth;

import java.time.LocalDateTime;
import java.time.ZoneId;

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
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_user_account_tokens")
public class UserAccountToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserAccountTokenType type;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "new_email")
    private String newEmail;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now(ZoneId.systemDefault());

    public UserAccountToken() {
        // JPA requires a no-args constructor
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public Long getId() {
        return id;
    }

    public String getNewEmail() {
        return newEmail;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public UserAccountTokenType getType() {
        return type;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public User getUser() {
        return user;
    }

    public boolean isExpired() {
        return LocalDateTime.now(ZoneId.systemDefault()).isAfter(expiresAt);
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isValid() {
        return !isUsed() && !isExpired();
    }

    public void markUsed() {
        this.usedAt = LocalDateTime.now(ZoneId.systemDefault());
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setNewEmail(String newEmail) {
        this.newEmail = newEmail;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public void setType(UserAccountTokenType type) {
        this.type = type;
    }

    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
