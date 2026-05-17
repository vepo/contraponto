package dev.vepo.contraponto.image;

import dev.vepo.contraponto.post.Post;
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
@Table(name = "tb_post_image_dependencies", uniqueConstraints = @UniqueConstraint(columnNames = { "post_id", "image_id", "role" }))
public class PostImageDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id", nullable = false)
    private Image image;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ImageRole role;

    public PostImageDependency() {}

    public PostImageDependency(Post post, Image image, ImageRole role) {
        this.post = post;
        this.image = image;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public Image getImage() {
        return image;
    }

    public Post getPost() {
        return post;
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

    public void setPost(Post post) {
        this.post = post;
    }

    public void setRole(ImageRole role) {
        this.role = role;
    }
}
