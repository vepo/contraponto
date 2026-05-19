package dev.vepo.contraponto.content.render;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.renderer.Format;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class PostContentRendererTest {

    @Inject
    PostContentRenderer renderer;

    @Test
    void rendersGithubLinkCard() {
        String content = "{% github https://github.com/vepo/imersao-kafka %}";
        assertThat(renderer.render(content, Format.MARKDOWN)).contains("github.com/vepo/imersao-kafka");
    }

    @Test
    void rendersYoutubePluginInMarkdownBody() {
        String content = "# Title\n\n{% youtube hPoHp0WhglA %}";
        assertThat(renderer.render(content, Format.MARKDOWN)).contains("youtube.com/embed/hPoHp0WhglA");
    }
}
