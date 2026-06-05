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
@Table(name = "tb_official_highlights")
public class OfficialHighlight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publication_id", nullable = false)
    private PostPublication publication;

    @Column(name = "anchor_cluster_hash", nullable = false, length = 64)
    private String anchorClusterHash;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String passage;

    @Column(name = "anchor_json", nullable = false, columnDefinition = "TEXT")
    private String anchorJson;

    @Column(name = "approved_at", nullable = false)
    private LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_user_id", nullable = false)
    private User approvedBy;

    @Column(name = "needs_review", nullable = false)
    private boolean needsReview;

    public OfficialHighlight() {}

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        OfficialHighlight other = (OfficialHighlight) obj;
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

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public User getApprovedBy() {
        return approvedBy;
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

    @Override
    public int hashCode() {
        return id == null ? System.identityHashCode(this) : Objects.hash(id);
    }

    public boolean isNeedsReview() {
        return needsReview;
    }

    @PrePersist
    void onCreate() {
        if (approvedAt == null) {
            approvedAt = LocalDateTime.now(ZoneId.systemDefault());
        }
    }

    public void setAnchorClusterHash(String anchorClusterHash) {
        this.anchorClusterHash = anchorClusterHash;
    }

    public void setAnchorJson(String anchorJson) {
        this.anchorJson = anchorJson;
    }

    public void setApprovedBy(User approvedBy) {
        this.approvedBy = approvedBy;
    }

    public void setNeedsReview(boolean needsReview) {
        this.needsReview = needsReview;
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
}
