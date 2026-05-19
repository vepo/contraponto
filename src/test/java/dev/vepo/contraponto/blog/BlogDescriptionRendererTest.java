package dev.vepo.contraponto.blog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
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
