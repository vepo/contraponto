package dev.vepo.contraponto.content.render;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class YoutubeContentRenderPluginTest {

    private final YoutubeContentRenderPlugin plugin = new YoutubeContentRenderPlugin();

    @Test
    void rejectsInvalidId() {
        assertThat(plugin.render(List.of("not-valid!!!"))).contains("content-render--error");
    }

    @Test
    void rendersEmbedForValidId() {
        assertThat(plugin.render(List.of("hPoHp0WhglA")))
                                                         .isEqualTo("""
                                                                    <iframe width="560" height="315" src="https://www.youtube.com/embed/hPoHp0WhglA" frameborder="0" allow="accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
                                                                    """
                                                                       .trim());
    }
}
