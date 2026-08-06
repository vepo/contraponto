package dev.vepo.contraponto.content.render;

import dev.vepo.contraponto.shared.UnitTest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

@UnitTest
class TwitterContentRenderPluginTest {

    private final TwitterContentRenderPlugin plugin = new TwitterContentRenderPlugin();

    @Test
    void normalizesXHostToCanonicalTwitterUrlInEmbed() {
        assertThat(plugin.render(List.of("https://www.x.com/vepo/status/1"))).contains("href=\"https://twitter.com/vepo/status/1\"");
    }

    @Test
    void parseTweetUrlAcceptsTwitterAndXHosts() {
        assertThat(TwitterContentRenderPlugin.parseTweetUrl("https://twitter.com/vepo/status/1787479135138230777"))
                                                                                                                   .contains("https://twitter.com/vepo/status/1787479135138230777");
        assertThat(TwitterContentRenderPlugin.parseTweetUrl("https://x.com/vepo/status/1787479135138230777"))
                                                                                                             .contains("https://twitter.com/vepo/status/1787479135138230777");
        assertThat(TwitterContentRenderPlugin.parseTweetUrl("https://www.twitter.com/vepo/status/1"))
                                                                                                     .contains("https://twitter.com/vepo/status/1");
    }

    @Test
    void rejectsEmptyParams() {
        assertThat(plugin.render(List.of())).contains("content-render--error").contains("URL required");
    }

    @Test
    void rejectsInvalidTweetUrl() {
        assertThat(TwitterContentRenderPlugin.parseTweetUrl("http://twitter.com/a/status/1")).isEmpty();
        assertThat(TwitterContentRenderPlugin.parseTweetUrl("https://example.com/a/status/1")).isEmpty();
        assertThat(TwitterContentRenderPlugin.parseTweetUrl("https://twitter.com/vepo/not-a-status/1")).isEmpty();
        assertThat(plugin.render(List.of("https://github.com/vepo/repo"))).contains("content-render--error");
    }

    @Test
    void rendersStaticEmbedForValidTweetUrl() {
        String url = "https://x.com/jack/status/20";

        String html = plugin.render(List.of(url));

        assertThat(html).contains("content-render--twitter")
                        .contains("twitter-tweet")
                        .contains("href=\"https://twitter.com/jack/status/20\"")
                        .contains("platform.twitter.com/widgets.js");
    }
}
