package dev.vepo.contraponto.shared.infra;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class TemplateExtensionsRenderTest {

    private User author;

    @Test
    void renderPublishedMarkdownIncludesHtml() {
        var post = Given.post()
                        .withAuthor(author)
                        .withBlog(author.getDefaultBlog())
                        .withTitle("Rendered")
                        .withSlug("rendered")
                        .withContent("# Heading")
                        .withPublished(true)
                        .persist();
        assertThat(TemplateExtensions.render(post)).contains("<h1");
    }

    @Test
    void renderPublishedMarkdownWithYoutubeEmbed() {
        var post = Given.post()
                        .withAuthor(author)
                        .withBlog(author.getDefaultBlog())
                        .withTitle("With embed")
                        .withSlug("with-embed")
                        .withContent("{% youtube hPoHp0WhglA %}")
                        .withPublished(true)
                        .persist();
        assertThat(TemplateExtensions.render(post)).contains("youtube.com/embed/hPoHp0WhglA");
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        author = Given.user()
                      .withUsername("renderuser")
                      .withEmail("renderuser@test.com")
                      .withName("Render User")
                      .withPassword("Password123!")
                      .persist();
    }
}
