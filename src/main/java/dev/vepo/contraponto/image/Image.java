package dev.vepo.contraponto.image;

import java.time.LocalDateTime;
import java.util.Objects;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_images")
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String uuid;

    @Column(nullable = false)
    private String filename;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(nullable = false)
    private Long size;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Image() {}

    public Image(String uuid, String filename, String contentType, Long size, String filePath, String url) {
        this.uuid = uuid;
        this.filename = filename;
        this.contentType = contentType;
        this.size = size;
        this.filePath = filePath;
        this.url = url;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Image other = (Image) obj;
        return Objects.equals(id, other.id);
    }

    public String getContentType() {
        return contentType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFilename() {
        return filename;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public Long getSize() {
        return size;
    }

    public String getUrl() {
        return url;
    }

    public String getUuid() {
        return uuid;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String toString() {
        return "Image[id=%d, uuid=%s, filename=%s, url=%s]".formatted(id, uuid, filename, url);
    }
}