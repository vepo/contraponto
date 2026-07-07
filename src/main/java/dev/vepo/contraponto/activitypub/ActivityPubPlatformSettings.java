package dev.vepo.contraponto.activitypub;

import java.time.LocalDateTime;
import java.time.ZoneId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_activitypub_platform_settings")
public class ActivityPubPlatformSettings {

    @Id
    private Integer id;

    @Column(name = "federation_enabled", nullable = false)
    private boolean federationEnabled;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public ActivityPubPlatformSettings() {}

    public ActivityPubPlatformSettings(Integer id, boolean federationEnabled) {
        this.id = id;
        this.federationEnabled = federationEnabled;
    }

    public Integer getId() {
        return id;
    }

    public boolean isFederationEnabled() {
        return federationEnabled;
    }

    public void setFederationEnabled(boolean federationEnabled) {
        this.federationEnabled = federationEnabled;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now(ZoneId.systemDefault());
    }
}
