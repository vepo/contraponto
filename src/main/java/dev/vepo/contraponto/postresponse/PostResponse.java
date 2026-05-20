package dev.vepo.contraponto.postresponse;

import java.time.LocalDateTime;
import java.util.Objects;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.user.User;
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
@Table(name = "tb_post_responses")
public class PostResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_post_id", nullable = false)
    private Post sourcePost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "response_post_id", nullable = false)
    private Post responsePost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responder_user_id", nullable = false)
    private User responder;

    @Enumerated(EnumType.STRING)
    @Column(name = "link_back_status", nullable = false, length = 20)
    private PostResponseLinkBackStatus linkBackStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    public PostResponse() {}

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PostResponse other = (PostResponse) obj;
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

    public PostResponseLinkBackStatus getLinkBackStatus() {
        return linkBackStatus;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public User getResponder() {
        return responder;
    }

    public Post getResponsePost() {
        return responsePost;
    }

    public Post getSourcePost() {
        return sourcePost;
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

    public void setLinkBackStatus(PostResponseLinkBackStatus linkBackStatus) {
        this.linkBackStatus = linkBackStatus;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public void setResponder(User responder) {
        this.responder = responder;
    }

    public void setResponsePost(Post responsePost) {
        this.responsePost = responsePost;
    }

    public void setSourcePost(Post sourcePost) {
        this.sourcePost = sourcePost;
    }
}
