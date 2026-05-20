package dev.vepo.contraponto.content.render;

import dev.vepo.contraponto.shared.UnitTest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

@UnitTest
class GithubContentRenderPluginTest {

    private final GithubContentRenderPlugin plugin = new GithubContentRenderPlugin();

    @Test
    void rejectsEmptyParams() {
        assertThat(plugin.render(List.of())).contains("content-render--error").contains("URL required");
    }

    @Test
    void rejectsInvalidRepoPath() {
        assertThat(plugin.render(List.of("https://github.com/only-one-segment"))).contains("content-render--error")
                                                                                 .contains("Invalid GitHub repository URL");
    }

    @Test
    void rejectsNonHttps() {
        assertThat(plugin.render(List.of("http://github.com/owner/repo"))).contains("content-render--error")
                                                                          .contains("https://github.com");
    }

    @Test
    void rejectsWrongHost() {
        assertThat(plugin.render(List.of("https://gitlab.com/owner/repo"))).contains("content-render--error")
                                                                           .contains("github.com");
    }

    @Test
    void rendersRepoLinkForValidUrl() {
        assertThat(plugin.render(List.of("https://github.com/owner/repo"))).contains("content-render--github")
                                                                           .contains("href=\"https://github.com/owner/repo\"")
                                                                           .contains("owner/repo");
    }
}
