package dev.vepo.contraponto.image;

import dev.vepo.contraponto.custompage.CustomPage;
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
@Table(name = "tb_custom_page_image_dependencies", uniqueConstraints = @UniqueConstraint(columnNames = { "custom_page_id", "image_id", "role" }))
public class CustomPageImageDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_page_id", nullable = false)
    private CustomPage customPage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id", nullable = false)
    private Image image;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ImageRole role;

    public CustomPageImageDependency() {}

    public CustomPageImageDependency(CustomPage customPage, Image image, ImageRole role) {
        this.customPage = customPage;
        this.image = image;
        this.role = role;
    }

    public CustomPage getCustomPage() {
        return customPage;
    }

    public Long getId() {
        return id;
    }

    public Image getImage() {
        return image;
    }

    public ImageRole getRole() {
        return role;
    }

    public void setCustomPage(CustomPage customPage) {
        this.customPage = customPage;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public void setRole(ImageRole role) {
        this.role = role;
    }
}
