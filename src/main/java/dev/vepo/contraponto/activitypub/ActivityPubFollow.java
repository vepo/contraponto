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
@Table(name = "tb_activitypub_follows")
public class ActivityPubFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "local_actor_id", nullable = false)
    private ActivityPubActor localActor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "remote_actor_id", nullable = false)
    private ActivityPubRemoteActor remoteActor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ActivityPubFollowStatus status;

    @Column(name = "follow_activity_id", length = 2048)
    private String followActivityId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    public ActivityPubFollow() {}

    public ActivityPubFollow(ActivityPubActor localActor,
                             ActivityPubRemoteActor remoteActor,
                             ActivityPubFollowStatus status,
                             String followActivityId) {
        this.localActor = localActor;
        this.remoteActor = remoteActor;
        this.status = status;
        this.followActivityId = followActivityId;
    }

    public void accept() {
        this.status = ActivityPubFollowStatus.ACCEPTED;
        this.acceptedAt = LocalDateTime.now(ZoneId.systemDefault());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ActivityPubFollow other = (ActivityPubFollow) obj;
        if (id == null || other.id == null) {
            return false;
        }
        return Objects.equals(other.id, id);
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getFollowActivityId() {
        return followActivityId;
    }

    public Long getId() {
        return id;
    }

    public ActivityPubActor getLocalActor() {
        return localActor;
    }

    public ActivityPubRemoteActor getRemoteActor() {
        return remoteActor;
    }

    public ActivityPubFollowStatus getStatus() {
        return status;
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
    }

    public void reject() {
        this.status = ActivityPubFollowStatus.REJECTED;
    }

    /**
     * Puts a previously rejected (or stale) follow back into
     * {@link ActivityPubFollowStatus#PENDING} so auto-accept can run again after a
     * remote unfollow + re-follow.
     */
    public void reopenAsPending(String newFollowActivityId) {
        this.status = ActivityPubFollowStatus.PENDING;
        this.acceptedAt = null;
        this.followActivityId = newFollowActivityId;
    }
}
