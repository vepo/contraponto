package dev.vepo.contraponto.highlight;

import java.time.LocalDateTime;
import java.util.Objects;

import dev.vepo.contraponto.post.Post;
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
@Table(name = "tb_common_highlight_proposals")
public class CommonHighlightProposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "anchor_cluster_hash", nullable = false, length = 64)
    private String anchorClusterHash;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String passage;

    @Column(name = "reader_count", nullable = false)
    private int readerCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProposalStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    public CommonHighlightProposal() {}

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        CommonHighlightProposal other = (CommonHighlightProposal) obj;
        if (id == null || other.id == null) {
            return false;
        }
        return Objects.equals(other.id, id);
    }

    public String getAnchorClusterHash() {
        return anchorClusterHash;
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

    public int getReaderCount() {
        return readerCount;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public ProposalStatus getStatus() {
        return status;
    }

    @Override
    public int hashCode() {
        return id == null ? System.identityHashCode(this) : Objects.hash(id);
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void setAnchorClusterHash(String anchorClusterHash) {
        this.anchorClusterHash = anchorClusterHash;
    }

    public void setPassage(String passage) {
        this.passage = passage;
    }

    public void setPost(Post post) {
        this.post = post;
    }

    public void setReaderCount(int readerCount) {
        this.readerCount = readerCount;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public void setStatus(ProposalStatus status) {
        this.status = status;
    }
}
