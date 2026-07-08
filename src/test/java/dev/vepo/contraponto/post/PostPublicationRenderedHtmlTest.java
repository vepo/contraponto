package dev.vepo.contraponto.post;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.content.render.PostContentRenderer;
import dev.vepo.contraponto.image.ContentImageMarkerService;
import dev.vepo.contraponto.image.ImageRepository;
import dev.vepo.contraponto.renderer.Format;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class PostPublicationRenderedHtmlTest {

    @Inject
    PostPublicationService publicationService;

    @Inject
    PostRepository postRepository;

    @Inject
    PostPublicationRepository publicationRepository;

    @Inject
    PostContentRenderer postContentRenderer;

    @Inject
    ContentImageMarkerService markerService;

    @Inject
    ImageRepository imageRepository;

    @Test
    void lazyBackfillPersistsRenderedHtmlForLegacyPublicationRows() {
        User author = Given.user()
                           .withUsername("legacypub")
                           .withEmail("legacypub@test.com")
                           .withName("Legacy Pub Author")
                           .withPassword("Password123!")
                           .persist();
        var post = Given.post()
                        .withAuthor(author)
                        .withTitle("Legacy publication")
                        .withSlug("legacy-publication")
                        .withContent("Legacy **markdown** body")
                        .withFormat(Format.MARKDOWN)
                        .persist();

        PostPublication live = publicationRepository.findLatestByPostId(post.getId()).orElseThrow();
        Given.transaction(() -> {
            var entityManager = Given.inject(jakarta.persistence.EntityManager.class);
            entityManager.createQuery("""
                                      UPDATE PostPublication p
                                      SET p.renderedHtml = null
                                      WHERE p.id = :id
                                      """)
                         .setParameter("id", live.getId())
                         .executeUpdate();
        });
        live.setRenderedHtml(null);

        String rendered = postContentRenderer.render(live);
        assertThat(rendered).contains("<strong>markdown</strong>");

        PostPublication reloaded = publicationRepository.findById(live.getId()).orElseThrow();
        assertThat(reloaded.getRenderedHtml()).isNotBlank();
        assertThat(reloaded.getRenderedHtml()).contains("<strong>markdown</strong>");
    }

    @Test
    void publishStoresRenderedHtmlSnapshot() {
        User author = Given.user()
                           .withUsername("renderhtml")
                           .withEmail("renderhtml@test.com")
                           .withName("Render HTML Author")
                           .withPassword("Password123!")
                           .persist();
        var post = Given.post()
                        .withAuthor(author)
                        .withTitle("Rendered HTML Post")
                        .withSlug("rendered-html-post")
                        .withContent("""
                                     Hello from AsciiDoc.

                                     {% youtube 1QMlkS53h7M %}
                                     """)
                        .withFormat(Format.ASCIIDOC)
                        .persist();

        Given.transaction(() -> {
            var reloaded = postRepository.findByIdWithTags(post.getId()).orElseThrow();
            publicationService.publish(reloaded);
        });

        PostPublication live = publicationRepository.findLatestByPostId(post.getId()).orElseThrow();
        assertThat(live.getRenderedHtml()).isNotBlank();
        assertThat(live.getRenderedHtml()).contains("Hello from AsciiDoc");
        assertThat(live.getRenderedHtml()).contains("youtube.com/embed/1QMlkS53h7M");
        assertThat(live.getRenderedHtml()).doesNotContain("{% youtube");
    }

    @Test
    void renderPublicationUsesStoredHtmlAndStillEnrichesImageAlt() {
        User author = Given.user()
                           .withUsername("renderalt")
                           .withEmail("renderalt@test.com")
                           .withName("Render Alt Author")
                           .withPassword("Password123!")
                           .persist();
        var blog = author.getDefaultBlog();
        var image = Given.randomCover(blog);

        String storedContent = markerService.toStoredContent("image::%s[]".formatted(image.getUrl()));
        var post = Given.post()
                        .withAuthor(author)
                        .withTitle("Alt enrichment post")
                        .withSlug("alt-enrichment-post")
                        .withContent(storedContent)
                        .withFormat(Format.ASCIIDOC)
                        .persist();

        PostPublication live = publicationRepository.findLatestByPostId(post.getId()).orElseThrow();
        assertThat(live.getRenderedHtml()).isNotBlank();

        Given.transaction(() -> {
            var reloaded = imageRepository.findByUuid(image.getUuid()).orElseThrow();
            reloaded.setAltText("Stored alt caption");
            imageRepository.update(reloaded);
            String rendered = postContentRenderer.render(live);
            assertThat(rendered).contains("alt=\"Stored alt caption\"");
        });
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
    }
}
