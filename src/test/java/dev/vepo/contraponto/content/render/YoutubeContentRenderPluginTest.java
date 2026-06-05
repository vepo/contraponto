package dev.vepo.contraponto.content.render;

import dev.vepo.contraponto.shared.UnitTest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

@UnitTest
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
                                                                    <div class="content-render content-render--youtube">
                                                                    <iframe src="https://www.youtube.com/embed/hPoHp0WhglA" title="YouTube video player" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" referrerpolicy="strict-origin-when-cross-origin" allowfullscreen></iframe>
                                                                    </div>
                                                                    """
                                                                       .trim());
    }
}
