package dev.vepo.contraponto.messaging;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

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
@Table(name = "tb_message_threads")
public class MessageThread {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiator_user_id", nullable = false)
    private User initiator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MessageThreadStatus status = MessageThreadStatus.OPEN;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    public MessageThread() {}

    public void close() {
        if (status == MessageThreadStatus.OPEN) {
            status = MessageThreadStatus.CLOSED;
            closedAt = LocalDateTime.now(ZoneId.systemDefault());
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MessageThread other = (MessageThread) obj;
        if (id == null || other.id == null) {
            return false;
        }
        return Objects.equals(other.id, id);
    }

    public void freeze() {
        if (status == MessageThreadStatus.OPEN) {
            status = MessageThreadStatus.FROZEN;
        }
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getId() {
        return id;
    }

    public User getInitiator() {
        return initiator;
    }

    public User getOtherParticipant(long userId) {
        if (initiator.getId().equals(userId)) {
            return recipient;
        }
        if (recipient.getId().equals(userId)) {
            return initiator;
        }
        return null;
    }

    public User getRecipient() {
        return recipient;
    }

    public MessageThreadStatus getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public int hashCode() {
        return id == null ? System.identityHashCode(this) : Objects.hash(id);
    }

    public boolean involvesUser(long userId) {
        return initiator.getId().equals(userId) || recipient.getId().equals(userId);
    }

    public boolean isParticipant(long userId) {
        return involvesUser(userId);
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now(ZoneId.systemDefault());
        }
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setInitiator(User initiator) {
        this.initiator = initiator;
    }

    public void setRecipient(User recipient) {
        this.recipient = recipient;
    }

    public void setStatus(MessageThreadStatus status) {
        this.status = status;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void thaw() {
        if (status == MessageThreadStatus.FROZEN) {
            status = MessageThreadStatus.OPEN;
        }
    }
}
