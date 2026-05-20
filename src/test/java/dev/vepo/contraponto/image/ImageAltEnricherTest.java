package dev.vepo.contraponto.image;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@QuarkusIntegrationTest
class ImageAltEnricherTest {

    @Inject
    ImageAltEnricher enricher;

    private User author;
    private Blog blog;
    private Image image;

    @Test
    void enrichHtmlAddsAltWhenImageHasAltText() {
        String html = "<p><img src=\"%s\" class=\"inline\"></p>".formatted(image.getUrl());
        Given.transaction(() -> {
            EntityManager em = Given.inject(EntityManager.class);
            Image stored = em.createQuery("FROM Image WHERE uuid = :uuid AND active = true", Image.class)
                             .setParameter("uuid", image.getUuid())
                             .getSingleResult();
            stored.setAltText("Diagram of flow");
            em.merge(stored);
            em.flush();
            assertThat(enricher.enrichHtml(html)).contains("alt=\"Diagram of flow\"");
        });
    }

    @Test
    void enrichHtmlLeavesHtmlWithoutImageUrlsUnchanged() {
        String html = "<p>Hello world</p>";
        assertThat(enricher.enrichHtml(html)).isEqualTo(html);
    }

    @Test
    void enrichHtmlReplacesExistingAltAttribute() {
        String html = "<img src=\"%s\" alt=\"old label\">".formatted(image.getUrl());
        Given.transaction(() -> {
            EntityManager em = Given.inject(EntityManager.class);
            Image stored = em.createQuery("FROM Image WHERE uuid = :uuid AND active = true", Image.class)
                             .setParameter("uuid", image.getUuid())
                             .getSingleResult();
            stored.setAltText("Updated description");
            em.merge(stored);
            em.flush();
            String enriched = enricher.enrichHtml(html);
            assertThat(enriched).contains("alt=\"Updated description\"");
            assertThat(enriched).doesNotContain("old label");
        });
    }

    @Test
    void enrichHtmlReturnsBlankUnchanged() {
        assertThat(enricher.enrichHtml("   ")).isEqualTo("   ");
    }

    @Test
    void enrichHtmlReturnsEmptyForNull() {
        assertThat(enricher.enrichHtml(null)).isEmpty();
    }

    @Test
    void enrichHtmlSkipsAltWhenImageHasNoAltText() {
        String html = "<img src=\"%s\">".formatted(image.getUrl());
        assertThat(enricher.enrichHtml(html)).isEqualTo(html);
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        author = Given.user()
                      .withUsername("altauthor")
                      .withEmail("altauthor@test.com")
                      .withName("Alt Author")
                      .withPassword("Password123!")
                      .persist();
        blog = author.getDefaultBlog();
        image = Given.randomCover(blog);
    }
}
