package dev.vepo.contraponto.serie;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class SerieServiceTest {

    @Inject
    SerieService serieService;

    @Inject
    SerieRepository serieRepository;

    private User author;
    private Blog blog;
    private Post post;

    @Test
    void applySerieTitleCreatesSerieWhenMissing() {
        serieService.applySerieTitleToPost(post, "  My Series  ");

        assertThat(post.getSerie()).isNotNull();
        assertThat(post.getSerie().getTitle()).isEqualTo("My Series");
        assertThat(post.getSerie().getSlug()).isEqualTo("my-series");
        assertThat(serieRepository.findByBlogIdAndSlug(blog.getId(), "my-series")).isPresent();
    }

    @Test
    void applySerieTitleReusesExistingSerie() {
        Given.transaction(() -> {
            var existing = serieRepository.persist(new Serie(blog, "Old title", "my-series"));
            serieService.applySerieTitleToPost(post, "My Series");
            assertThat(post.getSerie().getId()).isEqualTo(existing.getId());
            assertThat(post.getSerie().getTitle()).isEqualTo("My Series");
        });
    }

    @Test
    void blankSerieTitleClearsPostSerie() {
        Given.transaction(() -> {
            var existing = serieRepository.persist(new Serie(blog, "Existing", "existing"));
            post.setSerie(existing);
            serieService.applySerieTitleToPost(post, "   ");
            assertThat(post.getSerie()).isNull();
        });
    }

    @Test
    void invalidSlugClearsPostSerie() {
        serieService.applySerieTitleToPost(post, "---");
        assertThat(post.getSerie()).isNull();
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        author = Given.user()
                      .withUsername("serieauthor")
                      .withEmail("serieauthor@test.com")
                      .withName("Serie Author")
                      .withPassword("Password123!")
                      .persist();
        blog = author.getDefaultBlog();
        post = Given.post()
                    .withAuthor(author)
                    .withBlog(blog)
                    .withTitle("Draft")
                    .withSlug("draft-serie")
                    .withContent("Draft body")
                    .withPublished(false)
                    .persist();
    }
}
