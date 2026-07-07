package dev.vepo.contraponto.messaging;

import java.time.LocalDateTime;
import java.util.Objects;

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
@Table(name = "tb_message_thread_participants")
public class MessageThreadParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false)
    private MessageThread thread;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_read_message_id")
    private ThreadMessage lastReadMessage;

    public MessageThreadParticipant() {}

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MessageThreadParticipant other = (MessageThreadParticipant) obj;
        if (id == null || other.id == null) {
            return false;
        }
        return Objects.equals(other.id, id);
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getLastReadAt() {
        return lastReadAt;
    }

    public ThreadMessage getLastReadMessage() {
        return lastReadMessage;
    }

    public MessageThread getThread() {
        return thread;
    }

    public User getUser() {
        return user;
    }

    @Override
    public int hashCode() {
        return id == null ? System.identityHashCode(this) : Objects.hash(id);
    }

    public boolean isUnread(ThreadMessage latestMessage) {
        if (latestMessage == null) {
            return false;
        }
        if (lastReadMessage == null) {
            return true;
        }
        return latestMessage.getCreatedAt().isAfter(lastReadAt != null ? lastReadAt : lastReadMessage.getCreatedAt());
    }

    public void markRead(ThreadMessage message) {
        lastReadMessage = message;
        lastReadAt = message.getCreatedAt();
    }

    public void setLastReadAt(LocalDateTime lastReadAt) {
        this.lastReadAt = lastReadAt;
    }

    public void setLastReadMessage(ThreadMessage lastReadMessage) {
        this.lastReadMessage = lastReadMessage;
    }

    public void setThread(MessageThread thread) {
        this.thread = thread;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
