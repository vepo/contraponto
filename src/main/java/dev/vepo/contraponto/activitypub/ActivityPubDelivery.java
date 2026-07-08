package dev.vepo.contraponto.activitypub;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

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
@Table(name = "tb_activitypub_deliveries")
public class ActivityPubDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "local_actor_id", nullable = false)
    private ActivityPubActor localActor;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 16)
    private ActivityPubActivityType activityType;

    @Column(name = "object_id", nullable = false, length = 2048)
    private String objectId;

    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "target_inbox_url", nullable = false, length = 2048)
    private String targetInboxUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ActivityPubDeliveryStatus status;

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

    public ActivityPubDelivery() {}

    public ActivityPubDelivery(ActivityPubActor localActor,
                               ActivityPubActivityType activityType,
                               String objectId,
                               String payloadJson,
                               String targetInboxUrl) {
        this.localActor = localActor;
        this.activityType = activityType;
        this.objectId = objectId;
        this.payloadJson = payloadJson;
        this.targetInboxUrl = targetInboxUrl;
        this.status = ActivityPubDeliveryStatus.PENDING;
        this.attempts = 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ActivityPubDelivery other = (ActivityPubDelivery) obj;
        if (id == null || other.id == null) {
            return false;
        }
        return Objects.equals(other.id, id);
    }

    public ActivityPubActivityType getActivityType() {
        return activityType;
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

    public String getLastError() {
        return lastError;
    }

    public ActivityPubActor getLocalActor() {
        return localActor;
    }

    public LocalDateTime getNextRetryAt() {
        return nextRetryAt;
    }

    public String getObjectId() {
        return objectId;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public ActivityPubDeliveryStatus getStatus() {
        return status;
    }

    public String getTargetInboxUrl() {
        return targetInboxUrl;
    }

    @Override
    public int hashCode() {
        return id == null ? System.identityHashCode(this) : Objects.hash(id);
    }

    public void markDelivered() {
        this.status = ActivityPubDeliveryStatus.DELIVERED;
        this.deliveredAt = LocalDateTime.now(ZoneId.systemDefault());
        this.lastError = null;
    }

    /**
     * Records a failed send. Increments {@link #attempts}; keeps {@code PENDING}
     * for retry until the attempt limit, then marks
     * {@link ActivityPubDeliveryStatus#FAILED}. {@code error} must not be blank so
     * operators can diagnose via {@code last_error}.
     */
    public void markFailed(String error, LocalDateTime nextRetryAt) {
        this.attempts = attempts + 1;
        this.lastError = (error == null || error.isBlank()) ? "unknown delivery failure" : error;
        this.nextRetryAt = nextRetryAt;
        if (attempts >= 5) {
            this.status = ActivityPubDeliveryStatus.FAILED;
        } else {
            this.status = ActivityPubDeliveryStatus.PENDING;
        }
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now(ZoneId.systemDefault());
        }
        if (status == null) {
            status = ActivityPubDeliveryStatus.PENDING;
        }
    }
}
