package dev.vepo.contraponto.post;

import java.time.LocalDateTime;

import dev.vepo.contraponto.blog.Blog;
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
@Table(name = "tb_post_slug_aliases", uniqueConstraints = @UniqueConstraint(columnNames = { "blog_id", "slug" }))
public class PostSlugAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blog_id", nullable = false)
    private Blog blog;

    @Column(nullable = false)
    private String slug;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public PostSlugAlias() {}

    public Blog getBlog() {
        return blog;
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

    public String getSlug() {
        return slug;
    }

    public void setBlog(Blog blog) {
        this.blog = blog;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setPost(Post post) {
        this.post = post;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }
}
