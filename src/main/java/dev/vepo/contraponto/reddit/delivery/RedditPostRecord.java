package dev.vepo.contraponto.reddit.delivery;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

import dev.vepo.contraponto.post.Post;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_reddit_post_records")
public class RedditPostRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false, unique = true)
    private Post post;

    @Column(name = "submission_fullname", nullable = false, length = 32)
    private String submissionFullname;

    @Column(name = "permalink", length = 512)
    private String permalink;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public RedditPostRecord() {}

    public RedditPostRecord(Post post, String submissionFullname, String permalink) {
        this.post = post;
        this.submissionFullname = submissionFullname;
        this.permalink = permalink;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        RedditPostRecord other = (RedditPostRecord) obj;
        return Objects.equals(id, other.id);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getPermalink() {
        return permalink;
    }

    public Post getPost() {
        return post;
    }

    public String getSubmissionFullname() {
        return submissionFullname;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now(ZoneId.systemDefault());
        }
    }
}
