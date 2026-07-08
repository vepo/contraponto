package dev.vepo.contraponto.activitypub.inbox;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
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
import dev.vepo.contraponto.activitypub.remote.ActivityPubRemoteActor;
import dev.vepo.contraponto.post.Post;

@Entity
@Table(name = "tb_activitypub_favourites")
public class ActivityPubFavourite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "remote_actor_id", nullable = false)
    private ActivityPubRemoteActor remoteActor;

    @Column(name = "like_activity_id", nullable = false, unique = true, length = 2048)
    private String likeActivityId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public ActivityPubFavourite() {}

    public ActivityPubFavourite(Post post, ActivityPubRemoteActor remoteActor, String likeActivityId) {
        this.post = post;
        this.remoteActor = remoteActor;
        this.likeActivityId = likeActivityId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ActivityPubFavourite other = (ActivityPubFavourite) obj;
        if (id == null || other.id == null) {
            return false;
        }
        return Objects.equals(other.id, id);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getLikeActivityId() {
        return likeActivityId;
    }

    public Post getPost() {
        return post;
    }

    public ActivityPubRemoteActor getRemoteActor() {
        return remoteActor;
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
}
