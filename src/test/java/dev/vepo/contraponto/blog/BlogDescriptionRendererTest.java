package dev.vepo.contraponto.blog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class BlogDescriptionRendererTest {

    @Inject
    BlogDescriptionRenderer renderer;

    @Test
    void rendersMarkdownBold() {
        assertThat(renderer.render("**Bold** bio")).contains("<strong>Bold</strong>");
    }

    @Test
    void stripsIframeFromDescription() {
        String html = renderer.render("<iframe src=\"https://www.youtube.com/embed/x\"></iframe>");
        assertThat(html).doesNotContain("iframe");
    }
}
