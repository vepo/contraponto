package dev.vepo.contraponto.user;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.image.Image;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "pending_email")
    private String pendingEmail;

    @Column(nullable = false)
    private String name;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean active = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tb_user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Set<Role> roles = new HashSet<>();

    @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY)
    private Set<Blog> blogs;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_picture_id")
    private Image profilePicture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_banner_id")
    private Image defaultBlogBanner;

    @Column(name = "profile_description", columnDefinition = "TEXT")
    private String profileDescription;

    @Column(name = "website_url", length = 2048)
    private String websiteUrl;

    @Column(name = "twitter_url", length = 2048)
    private String twitterUrl;

    @Column(name = "mastodon_url", length = 2048)
    private String mastodonUrl;

    @Column(name = "github_url", length = 2048)
    private String githubUrl;

    @Column(name = "linkedin_url", length = 2048)
    private String linkedinUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public User() {}

    public User(String username, String email, String name, Set<Role> roles, String passwordHash) {
        this.username = username;
        this.email = email;
        this.name = name;
        this.roles = new HashSet<>(roles);
        this.passwordHash = passwordHash;
        this.active = true;
        this.blogs = new HashSet<>();
    }

    public void addRole(Role role) {
        if (roles == null) {
            roles = new HashSet<>();
        }
        roles.add(role);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        var other = (User) obj;
        return Objects.equals(other.id, id);
    }

    public Set<Blog> getBlogs() {
        return blogs;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Blog getDefaultBlog() {
        return blogs.stream()
                    .filter(Blog::isMain)
                    .findFirst()
                    .orElse(null);
    }

    public Image getDefaultBlogBanner() {
        return defaultBlogBanner;
    }

    public String getEmail() {
        return email;
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public Long getId() {
        return id;
    }

    public String getLinkedinUrl() {
        return linkedinUrl;
    }

    public String getMastodonUrl() {
        return mastodonUrl;
    }

    public String getName() {
        return name;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getPendingEmail() {
        return pendingEmail;
    }

    public String getProfileDescription() {
        return profileDescription;
    }

    public Image getProfilePicture() {
        return profilePicture;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public String getTwitterUrl() {
        return twitterUrl;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getUsername() {
        return username;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public boolean hasRole(Role role) {
        return roles != null && roles.contains(role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setBlogs(Set<Blog> blogs) {
        this.blogs = blogs;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setDefaultBlogBanner(Image defaultBlogBanner) {
        this.defaultBlogBanner = defaultBlogBanner;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setGithubUrl(String githubUrl) {
        this.githubUrl = githubUrl;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setLinkedinUrl(String linkedinUrl) {
        this.linkedinUrl = linkedinUrl;
    }

    public void setMastodonUrl(String mastodonUrl) {
        this.mastodonUrl = mastodonUrl;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setPendingEmail(String pendingEmail) {
        this.pendingEmail = pendingEmail;
    }

    public void setProfileDescription(String profileDescription) {
        this.profileDescription = profileDescription;
    }

    public void setProfilePicture(Image profilePicture) {
        this.profilePicture = profilePicture;
    }

    public void setRoles(Collection<Role> roles) {
        this.roles = new HashSet<>(roles);
    }

    public void setTwitterUrl(String twitterUrl) {
        this.twitterUrl = twitterUrl;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    @Override
    public String toString() {
        return "User[id=%d, username=%s, email=%s, name=%s]".formatted(id, username, email, name);
    }
}
