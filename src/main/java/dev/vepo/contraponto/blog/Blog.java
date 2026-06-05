package dev.vepo.contraponto.blog;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

import org.hibernate.annotations.CreationTimestamp;

import dev.vepo.contraponto.image.Image;
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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "tb_blogs", uniqueConstraints = @UniqueConstraint(columnNames = { "owner_id", "slug" }))
public class Blog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String slug; // URL-friendly, unique per user

    @Column
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private boolean main;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "git_enabled", nullable = false)
    private boolean gitEnabled;

    @Column(name = "git_remote_url", length = 2048)
    private String gitRemoteUrl;

    @Column(name = "git_branch", nullable = false, length = 255)
    private String gitBranch = "main";

    @Column(name = "git_last_known_commit", length = 64)
    private String gitLastKnownCommit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banner_id")
    private Image banner;

    public Blog() {}

    public Blog(User user) {
        this.slug = user.getUsername();
        this.name = user.getName();
        this.description = user.getName();
        this.owner = user;
        this.main = true;
        this.active = true;
        this.createdAt = LocalDateTime.now(ZoneId.systemDefault());
    }

    public Blog(User user, String slug, String name, String description) {
        this.owner = user;
        this.slug = slug;
        this.name = name;
        this.description = description;
        this.main = false;
        this.active = true;
        this.createdAt = LocalDateTime.now(ZoneId.systemDefault());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Blog other = (Blog) obj;
        return Objects.equals(id, other.id);
    }

    public Image getBanner() {
        return banner;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getDescription() {
        return description;
    }

    public String getGitBranch() {
        return gitBranch;
    }

    public String getGitLastKnownCommit() {
        return gitLastKnownCommit;
    }

    public String getGitRemoteUrl() {
        return gitRemoteUrl;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public User getOwner() {
        return owner;
    }

    public String getSlug() {
        return slug;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public boolean isActive() {
        return active;
    }

    public boolean isGitEnabled() {
        return gitEnabled;
    }

    public boolean isMain() {
        return main;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setBanner(Image banner) {
        this.banner = banner;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setGitBranch(String gitBranch) {
        this.gitBranch = gitBranch == null || gitBranch.isBlank() ? "main" : gitBranch.trim();
    }

    public void setGitEnabled(boolean gitEnabled) {
        this.gitEnabled = gitEnabled;
    }

    public void setGitLastKnownCommit(String gitLastKnownCommit) {
        this.gitLastKnownCommit = gitLastKnownCommit;
    }

    public void setGitRemoteUrl(String gitRemoteUrl) {
        if (gitRemoteUrl == null) {
            this.gitRemoteUrl = null;
        } else {
            String trimmed = gitRemoteUrl.trim();
            this.gitRemoteUrl = trimmed.isEmpty() ? null : trimmed;
        }
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setMain(boolean main) {
        this.main = main;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    @Override
    public String toString() {
        return "Blog [id=%d, name=%s, slug=%s, description=%s, owner=%s, main=%b, active=%b, gitEnabled=%b, createdAt=%s]".formatted(id, name, slug,
                                                                                                                                     description,
                                                                                                                                     owner,
                                                                                                                                     main, active,
                                                                                                                                     gitEnabled,
                                                                                                                                     createdAt);
    }

}