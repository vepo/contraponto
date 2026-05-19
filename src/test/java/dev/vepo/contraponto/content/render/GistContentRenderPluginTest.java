package dev.vepo.contraponto.content.render;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class GistContentRenderPluginTest {

    private final GistContentRenderPlugin plugin = new GistContentRenderPlugin();

    @Test
    void rejectsEmptyParams() {
        assertThat(plugin.render(List.of())).contains("content-render--error").contains("Gist URL required");
    }

    @Test
    void rejectsInvalidGistId() {
        assertThat(plugin.render(List.of("https://gist.github.com/user/not-hex-id"))).contains("content-render--error")
                                                                                     .contains("Invalid Gist URL");
    }

    @Test
    void rejectsNonGistHost() {
        assertThat(plugin.render(List.of("https://github.com/owner/repo"))).contains("content-render--error")
                                                                           .contains("gist.github.com");
    }

    @Test
    void rendersScriptSrc() {
        String id = "b63ff8384941329485266999f99e2264";
        assertThat(plugin.render(List.of("https://gist.github.com/vepo/" + id))).contains("content-render--gist")
                                                                                .contains("gist.github.com/vepo/" + id + ".js");
    }
}
