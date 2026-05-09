// dev/vepo/contraponto/custompage/CustomPage.java
package dev.vepo.contraponto.custompage;

import java.time.LocalDateTime;
import java.util.Objects;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
@Table(name = "tb_custom_pages", uniqueConstraints = { @UniqueConstraint(columnNames = { "slug", "blog_owner_id" })
})
public class CustomPage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String slug; // URL-friendly identifier

    @Column
    private String section;

    @Column(columnDefinition = "TEXT")
    private String content; // rendered HTML (or original markdown/asciidoc)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PagePlacement placement = PagePlacement.NONE;

    @Column(nullable = false)
    private boolean published = true;

    // If null, the page is global (site-wide).
    // If set, it belongs to a specific user's blog.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blog_id")
    private User blog;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public CustomPage() {}

    public CustomPage(String slug, String title, String section, String content, PagePlacement placement, User blog, boolean published) {
        this.slug = slug;
        this.title = title;
        this.section = section;
        this.content = content;
        this.placement = placement;
        this.blog = blog;
        this.published = published;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            CustomPage other = (CustomPage) obj;
            return Objects.equals(other.id, id);
        }
    }

    public User getBlog() {
        return blog;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getId() {
        return id;
    }

    public PagePlacement getPlacement() {
        return placement;
    }

    public String getSection() {
        return section;
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

    public boolean isPublished() {
        return published;
    }

    public void setBlog(User blog) {
        this.blog = blog;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setPlacement(PagePlacement placement) {
        this.placement = placement;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public void setSection(String section) {
        this.section = section;
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
        return "CustomPage[id=%d, slug=%s, section=%s]".formatted(id, slug, section);
    }
}