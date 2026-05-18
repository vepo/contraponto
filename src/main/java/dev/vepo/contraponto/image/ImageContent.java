package dev.vepo.contraponto.image;

import java.util.Objects;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_image_content")
public class ImageContent {

    @Id
    @Column(name = "image_id")
    private Long imageId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id", insertable = false, updatable = false)
    private Image image;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(nullable = false)
    private byte[] content;

    public ImageContent() {}

    public ImageContent(Image image, byte[] content) {
        this.imageId = image.getId();
        this.image = image;
        this.content = content;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ImageContent other = (ImageContent) obj;
        return Objects.equals(imageId, other.imageId);
    }

    public byte[] getContent() {
        return content;
    }

    public Image getImage() {
        return image;
    }

    public Long getImageId() {
        return imageId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(imageId);
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public void setImage(Image image) {
        this.image = image;
        this.imageId = image != null ? image.getId() : null;
    }

    public void setImageId(Long imageId) {
        this.imageId = imageId;
    }
}
