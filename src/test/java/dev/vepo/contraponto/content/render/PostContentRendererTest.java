package dev.vepo.contraponto.content.render;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.image.ContentImageMarkerService;
import dev.vepo.contraponto.image.Image;
import dev.vepo.contraponto.renderer.Format;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class PostContentRendererTest {

    @Inject
    PostContentRenderer renderer;

    @Inject
    ContentImageMarkerService markerService;

    private Blog blog;
    private Image image;

    @Test
    void rendersAsciiDocBlockTitleImageWhenStoredContentHasImageMarker() {
        String caption = "Tipos de armazenamentos possíveis para bases de dados";
        String editor = """
                        .%s
                        image::%s[]
                        """.formatted(caption, image.getUrl());
        String stored = markerService.toStoredContent(editor);

        String html = renderer.render(stored, Format.ASCIIDOC);

        assertThat(html).contains("src=\"" + image.getUrl() + "\"");
        assertThat(html).contains(caption);
        assertThat(html).doesNotContain("Figure 1.");
        assertThat(html).contains("imageblock");
        assertThat(html).doesNotContain("contraponto:image");
        assertThat(html).doesNotContain("image::");
        assertThat(html).doesNotContain("&lt;!--");
    }

    @Test
    void rendersAsciiDocBlockTitleImageWithAttributeListWhenStoredContentHasImageMarker() {
        String editor = """
                        .A mountain sunset
                        [#img-sunset,link=https://example.com/photo]
                        image::%s[Sunset,200,100]
                        """.formatted(image.getUrl());
        String stored = markerService.toStoredContent(editor);

        String html = renderer.render(stored, Format.ASCIIDOC);

        assertThat(html).contains("src=\"" + image.getUrl() + "\"");
        assertThat(html).contains("A mountain sunset");
        assertThat(html).doesNotContain("contraponto:image");
    }

    @Test
    void rendersAsciiDocQuoteBlockWithAttribution() {
        String content = """
                         [quote, Anthropic CEO, Dario Amodei]
                         ____
                         We might be 6-12 months away from models doing all of what Software Engineers do End-to-End.
                         ____
                         """;

        String html = renderer.render(content, Format.ASCIIDOC);

        assertThat(html).contains("quoteblock");
        assertThat(html).contains("<blockquote>");
        assertThat(html).contains("class=\"attribution\"");
        assertThat(html).contains("Anthropic CEO");
        assertThat(html).contains("<cite>Dario Amodei</cite>");
    }

    @Test
    void rendersGithubLinkCard() {
        String content = "{% github https://github.com/vepo/imersao-kafka %}";
        assertThat(renderer.render(content, Format.MARKDOWN)).contains("github.com/vepo/imersao-kafka");
    }

    @Test
    void rendersMarkdownImageWithDoubleSlashApiPathFromLegacyImport() {
        String caption = "Etapas do ATAM";
        String stored = """
                        <!-- contraponto:image uuid="%s" -->
                        ![%s](//api/images/%s.png)
                        """.formatted(image.getUuid(), caption, image.getUuid());

        String html = renderer.render(stored, Format.MARKDOWN);

        assertThat(html).contains("src=\"/api/images/" + image.getUuid() + ".png\"");
        assertThat(html).doesNotContain("//api/images/");
        assertThat(html).contains(caption);
    }

    @Test
    void rendersYoutubePluginInMarkdownBody() {
        String content = "# Title\n\n{% youtube hPoHp0WhglA %}";
        assertThat(renderer.render(content, Format.MARKDOWN)).contains("youtube.com/embed/hPoHp0WhglA");
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        User author = Given.user()
                           .withUsername("renderimg")
                           .withEmail("renderimg@test.com")
                           .withName("Render Image Author")
                           .withPassword("Password123!")
                           .persist();
        blog = author.getDefaultBlog();
        image = Given.randomCover(blog);
    }
}
