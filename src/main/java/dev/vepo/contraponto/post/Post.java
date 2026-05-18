package dev.vepo.contraponto.post;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.image.Image;
import dev.vepo.contraponto.serie.Serie;
import dev.vepo.contraponto.renderer.Format;
import dev.vepo.contraponto.tag.Tag;
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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "tb_posts", uniqueConstraints = { @UniqueConstraint(columnNames = { "author", "slug" }) })
public class Post {
    public static final class Builder {
        private String title;
        private Image cover;
        private String slug;
        private String description;
        private String content;
        private Format format = Format.MARKDOWN;
        private Blog blog;
        private boolean published;
        private boolean featured;
        private LocalDateTime publishedAt;

        public Builder blog(Blog blog) {
            this.blog = blog;
            return this;
        }

        public Post build() {
            var post = new Post();
            post.setTitle(title);
            post.setCover(cover);
            post.setSlug(slug);
            post.setDescription(description);
            post.setContent(content);
            post.setFormat(format);
            post.setBlog(blog);
            post.setPublished(published);
            post.setFeatured(featured);
            post.setPublishedAt(publishedAt);
            return post;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder cover(Image cover) {
            this.cover = cover;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder featured(boolean featured) {
            this.featured = featured;
            return this;
        }

        public Builder format(Format format) {
            this.format = format;
            return this;
        }

        public Builder published(boolean published) {
            this.published = published;
            return this;
        }

        public Builder publishedAt(LocalDateTime publishedAt) {
            this.publishedAt = publishedAt;
            return this;
        }

        public Builder slug(String slug) {
            this.slug = slug;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "serie_id")
    private Serie serie;

    @ManyToMany
    @JoinTable(name = "tb_post_tags", joinColumns = @JoinColumn(name = "post_id"), inverseJoinColumns = @JoinColumn(name = "tag_id"))
    @OrderBy("name ASC")
    private List<Tag> tags = new ArrayList<>();

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "live_publication_id")
    private PostPublication livePublication;

    public Post() {}

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

    public PostPublication getLivePublication() {
        return livePublication;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public Serie getSerie() {
        return serie;
    }

    public String getSlug() {
        return slug;
    }

    public List<Tag> getTags() {
        return tags;
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

    public void setLivePublication(PostPublication livePublication) {
        this.livePublication = livePublication;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public void setSerie(Serie serie) {
        this.serie = serie;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
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
