package dev.vepo.contraponto.reddit.admin;

import java.time.LocalDateTime;
import java.time.ZoneId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_reddit_platform_settings")
public class RedditPlatformSettings {

    @Id
    private Integer id;

    @Column(name = "syndication_enabled", nullable = false)
    private boolean syndicationEnabled;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public RedditPlatformSettings() {}

    public RedditPlatformSettings(int id, boolean syndicationEnabled) {
        this.id = id;
        this.syndicationEnabled = syndicationEnabled;
        this.updatedAt = LocalDateTime.now(ZoneId.systemDefault());
    }

    public Integer getId() {
        return id;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isSyndicationEnabled() {
        return syndicationEnabled;
    }

    public void setSyndicationEnabled(boolean syndicationEnabled) {
        this.syndicationEnabled = syndicationEnabled;
        this.updatedAt = LocalDateTime.now(ZoneId.systemDefault());
    }
}
