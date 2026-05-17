package dev.vepo.contraponto.image;

import dev.vepo.contraponto.post.PostPublication;
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
@Table(name = "tb_post_publication_image_dependencies", uniqueConstraints = @UniqueConstraint(columnNames = { "publication_id", "image_id", "role" }))
public class PostPublicationImageDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publication_id", nullable = false)
    private PostPublication publication;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id", nullable = false)
    private Image image;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ImageRole role;

    public PostPublicationImageDependency() {}

    public PostPublicationImageDependency(PostPublication publication, Image image, ImageRole role) {
        this.publication = publication;
        this.image = image;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public Image getImage() {
        return image;
    }

    public PostPublication getPublication() {
        return publication;
    }

    public ImageRole getRole() {
        return role;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public void setPublication(PostPublication publication) {
        this.publication = publication;
    }

    public void setRole(ImageRole role) {
        this.role = role;
    }
}
