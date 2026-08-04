package dev.vepo.contraponto.reaction;

import java.time.LocalDateTime;
import java.time.ZoneId;
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
@Table(name = "tb_post_reactions")
public class PostReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "reaction_type", nullable = false, length = 20)
    private ReactionType reactionType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public PostReaction() {
        // JPA requires a no-args constructor
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PostReaction other = (PostReaction) obj;
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

    public Post getPost() {
        return post;
    }

    public ReactionType getReactionType() {
        return reactionType;
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

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setPost(Post post) {
        this.post = post;
    }

    public void setReactionType(ReactionType reactionType) {
        this.reactionType = reactionType;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
