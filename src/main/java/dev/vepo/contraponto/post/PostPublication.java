package dev.vepo.contraponto.post;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import dev.vepo.contraponto.image.Image;
import dev.vepo.contraponto.renderer.Format;
import dev.vepo.contraponto.tag.Tag;
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
@Table(name = "tb_post_publications", uniqueConstraints = @UniqueConstraint(columnNames = { "post_id", "version" }))
public class PostPublication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(nullable = false)
    private int version;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @Column(nullable = false)
    private String slug;

    @Column
    private String title;

    @Column
    private String description;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * Sanitized HTML snapshot of {@link #content} at publish time (before
     * per-request image alt enrichment). Null for rows created before rendered-html
     * caching.
     */
    @Column(name = "rendered_html", columnDefinition = "TEXT")
    private String renderedHtml;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Format format;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cover_id")
    private Image cover;

    @ManyToMany
    @JoinTable(name = "tb_post_publication_tags", joinColumns = @JoinColumn(name = "publication_id"), inverseJoinColumns = @JoinColumn(name = "tag_id"))
    @OrderBy("name ASC")
    private List<Tag> tags = new ArrayList<>();

    // Required by JPA
    public PostPublication() {}

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PostPublication other = (PostPublication) obj;
        return Objects.equals(other.id, id);
    }

    public String getContent() {
        return content;
    }

    public Image getCover() {
        return cover;
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

    public Post getPost() {
        return post;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public String getRenderedHtml() {
        return renderedHtml;
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

    public int getVersion() {
        return version;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setCover(Image cover) {
        this.cover = cover;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setFormat(Format format) {
        this.format = format;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setPost(Post post) {
        this.post = post;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public void setRenderedHtml(String renderedHtml) {
        this.renderedHtml = renderedHtml;
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

    public void setVersion(int version) {
        this.version = version;
    }
}
