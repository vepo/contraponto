package dev.vepo.contraponto.highlight;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostPublication;
import dev.vepo.contraponto.user.User;
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

@Entity
@Table(name = "tb_post_text_highlights")
public class PostTextHighlight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publication_id", nullable = false)
    private PostPublication publication;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String passage;

    @Column(name = "anchor_json", nullable = false, columnDefinition = "TEXT")
    private String anchorJson;

    @Column(name = "anchor_cluster_hash", nullable = false, length = 64)
    private String anchorClusterHash;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public PostTextHighlight() {}

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PostTextHighlight other = (PostTextHighlight) obj;
        if (id == null || other.id == null) {
            return false;
        }
        return Objects.equals(other.id, id);
    }

    public String getAnchorClusterHash() {
        return anchorClusterHash;
    }

    public String getAnchorJson() {
        return anchorJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getPassage() {
        return passage;
    }

    public Post getPost() {
        return post;
    }

    public PostPublication getPublication() {
        return publication;
    }

    public User getUser() {
        return user;
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

    public void setAnchorClusterHash(String anchorClusterHash) {
        this.anchorClusterHash = anchorClusterHash;
    }

    public void setAnchorJson(String anchorJson) {
        this.anchorJson = anchorJson;
    }

    public void setPassage(String passage) {
        this.passage = passage;
    }

    public void setPost(Post post) {
        this.post = post;
    }

    public void setPublication(PostPublication publication) {
        this.publication = publication;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
