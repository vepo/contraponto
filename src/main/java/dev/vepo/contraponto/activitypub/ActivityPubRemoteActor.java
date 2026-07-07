package dev.vepo.contraponto.activitypub;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_activitypub_remote_actors")
public class ActivityPubRemoteActor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_id", nullable = false, unique = true, length = 2048)
    private String actorId;

    @Column(name = "inbox_url", nullable = false, length = 2048)
    private String inboxUrl;

    @Column(name = "public_key_pem", columnDefinition = "TEXT")
    private String publicKeyPem;

    @Column(name = "public_key_id", length = 2048)
    private String publicKeyId;

    @Column(name = "profile_fetched_at")
    private LocalDateTime profileFetchedAt;

    @Column(name = "display_name", length = 512)
    private String displayName;

    @Column(name = "preferred_username", length = 255)
    private String preferredUsername;

    public ActivityPubRemoteActor() {}

    public ActivityPubRemoteActor(String actorId, String inboxUrl) {
        this.actorId = actorId;
        this.inboxUrl = inboxUrl;
    }

    public void applyFetchedProfile(String inboxUrl,
                                    String publicKeyPem,
                                    String publicKeyId,
                                    String displayName,
                                    String preferredUsername) {
        this.inboxUrl = inboxUrl;
        this.publicKeyPem = publicKeyPem;
        this.publicKeyId = publicKeyId;
        this.displayName = displayName;
        this.preferredUsername = preferredUsername;
        this.profileFetchedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ActivityPubRemoteActor other = (ActivityPubRemoteActor) obj;
        if (id == null || other.id == null) {
            return false;
        }
        return Objects.equals(other.id, id);
    }

    public String getActorId() {
        return actorId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Long getId() {
        return id;
    }

    public String getInboxUrl() {
        return inboxUrl;
    }

    public String getPreferredUsername() {
        return preferredUsername;
    }

    public LocalDateTime getProfileFetchedAt() {
        return profileFetchedAt;
    }

    public String getPublicKeyId() {
        return publicKeyId;
    }

    public String getPublicKeyPem() {
        return publicKeyPem;
    }

    @Override
    public int hashCode() {
        return id == null ? System.identityHashCode(this) : Objects.hash(id);
    }

    public void setInboxUrl(String inboxUrl) {
        this.inboxUrl = inboxUrl;
    }

    public void updatePublicKey(String publicKeyPem, String publicKeyId) {
        this.publicKeyPem = publicKeyPem;
        this.publicKeyId = publicKeyId;
        this.profileFetchedAt = LocalDateTime.now();
    }
}
