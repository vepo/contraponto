package dev.vepo.contraponto.activitypub.actor;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import dev.vepo.contraponto.user.User;

@Entity
@Table(name = "tb_activitypub_actors")
public class ActivityPubActor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "federation_enabled", nullable = false)
    private boolean federationEnabled;

    @Column(name = "private_key_encrypted", nullable = false, columnDefinition = "TEXT")
    private String privateKeyEncrypted;

    @Column(name = "public_key_pem", nullable = false, columnDefinition = "TEXT")
    private String publicKeyPem;

    @Column(name = "public_key_id", nullable = false, length = 2048)
    private String publicKeyId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public ActivityPubActor() {}

    public ActivityPubActor(User user,
                            boolean federationEnabled,
                            String privateKeyEncrypted,
                            String publicKeyPem,
                            String publicKeyId) {
        this.user = user;
        this.federationEnabled = federationEnabled;
        this.privateKeyEncrypted = privateKeyEncrypted;
        this.publicKeyPem = publicKeyPem;
        this.publicKeyId = publicKeyId;
    }

    public void disableFederation() {
        this.federationEnabled = false;
    }

    public void enableFederation() {
        this.federationEnabled = true;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ActivityPubActor other = (ActivityPubActor) obj;
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

    public String getPrivateKeyEncrypted() {
        return privateKeyEncrypted;
    }

    public String getPublicKeyId() {
        return publicKeyId;
    }

    public String getPublicKeyPem() {
        return publicKeyPem;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public User getUser() {
        return user;
    }

    @Override
    public int hashCode() {
        return id == null ? System.identityHashCode(this) : Objects.hash(id);
    }

    public boolean isFederationEnabled() {
        return federationEnabled;
    }

    @PrePersist
    void onCreate() {
        var now = LocalDateTime.now(ZoneId.systemDefault());
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now(ZoneId.systemDefault());
    }

    public void rotateKeys(String privateKeyEncrypted, String publicKeyPem, String publicKeyId) {
        this.privateKeyEncrypted = privateKeyEncrypted;
        this.publicKeyPem = publicKeyPem;
        this.publicKeyId = publicKeyId;
    }

    public void setFederationEnabled(boolean federationEnabled) {
        this.federationEnabled = federationEnabled;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
