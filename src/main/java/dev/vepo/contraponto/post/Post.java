package dev.vepo.contraponto.post;

import java.time.LocalDateTime;
import java.util.Objects;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.image.Image;
import dev.vepo.contraponto.renderer.Format;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "tb_posts", uniqueConstraints = { @UniqueConstraint(columnNames = { "author", "slug" }) })
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Meaning https://en.wikipedia.org/wiki/Slug_(publishing)
    @Column(nullable = false)
    private String slug;

    @Column(nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cover_id")
    private Image cover;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blog_id", nullable = false)
    private Blog blog;

    @Column
    private String description;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Format format;

    @Column
    private boolean published;

    @Column(nullable = false)
    private boolean featured = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at", nullable = true)
    private LocalDateTime publishedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Post() {}

    public Post(String title,
                Image cover,
                String slug,
                String description,
                String content,
                Format format,
                Blog blog,
                boolean published,
                boolean featured,
                LocalDateTime publishedAt) {
        this.title = title;
        this.cover = cover;
        this.slug = slug;
        this.description = description;
        this.content = content;
        this.format = format;
        this.blog = blog;
        this.published = published;
        this.featured = featured;
        this.publishedAt = publishedAt;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            Post other = (Post) obj;
            return Objects.equals(other.id, id);
        }
    }

    public User getAuthor() {
        return blog.getOwner();
    }

    public Blog getBlog() {
        return blog;
    }

    public String getContent() {
        return content;
    }

    public Image getCover() {
        return cover;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getDescription() {
        return description;
    }

    public Format getFormat() {
        return format;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public String getSlug() {
        return slug;
    }

    public String getTitle() {
        return title;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public boolean isFeatured() {
        return featured;
    }

    public boolean isPublished() {
        return published;
    }

    public void setBlog(Blog blog) {
        this.blog = blog;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setCover(Image cover) {
        this.cover = cover;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    public void setFormat(Format format) {
        this.format = format;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Post[id=%d, slug=%s, blog=%s]".formatted(id, slug, blog);
    }
}
