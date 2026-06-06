package dev.vepo.contraponto.auth;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_account_email_outbox")
public class AccountEmailOutbox {

    public static AccountEmailOutbox pending(AccountEmailKind kind,
                                             String recipient,
                                             String subject,
                                             String payload,
                                             String lastError) {
        var entry = new AccountEmailOutbox();
        entry.kind = kind;
        entry.recipient = recipient;
        entry.subject = subject;
        entry.payload = payload;
        entry.attemptCount = 0;
        entry.nextRetryAt = LocalDateTime.now(ZoneId.systemDefault());
        entry.lastError = truncate(lastError);
        return entry;
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 2000 ? value : value.substring(0, 2000);
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AccountEmailKind kind;

    @Column(nullable = false)
    private String recipient;

    @Column(nullable = false, length = 512)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_retry_at", nullable = false)
    private LocalDateTime nextRetryAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    public AccountEmailOutbox() {
        // JPA requires a no-args constructor
    }

    void applyRetryFailure(int attemptCount, LocalDateTime nextRetryAt, String error) {
        this.attemptCount = attemptCount;
        this.nextRetryAt = nextRetryAt;
        this.lastError = truncate(error);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        AccountEmailOutbox other = (AccountEmailOutbox) obj;
        if (id == null || other.id == null) {
            return false;
        }
        return Objects.equals(other.id, id);
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getId() {
        return id;
    }

    public AccountEmailKind getKind() {
        return kind;
    }

    public String getLastError() {
        return lastError;
    }

    public LocalDateTime getNextRetryAt() {
        return nextRetryAt;
    }

    public String getPayload() {
        return payload;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getSubject() {
        return subject;
    }

    @Override
    public int hashCode() {
        return id == null ? System.identityHashCode(this) : Objects.hash(id);
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now(ZoneId.systemDefault());
        }
        if (nextRetryAt == null) {
            nextRetryAt = createdAt;
        }
    }
}
