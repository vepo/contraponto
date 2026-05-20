package dev.vepo.contraponto.content.render;

import dev.vepo.contraponto.shared.UnitTest;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

@UnitTest
class TwitterContentRenderPluginTest {

    private static String jsonString(String value) {
        return "\"" + value.replace("\\", "\\\\")
                           .replace("\"", "\\\"")
                           .replace("\n", "\\n")
                           .replace("\r", "")
                + "\"";
    }

    private HttpServer server;

    private TwitterContentRenderPlugin plugin;

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
    void rejectsInvalidTweetUrl() {
        assertThat(TwitterContentRenderPlugin.parseTweetUrl("http://twitter.com/a/status/1")).isEmpty();
        assertThat(TwitterContentRenderPlugin.parseTweetUrl("https://example.com/a/status/1")).isEmpty();
        assertThat(TwitterContentRenderPlugin.parseTweetUrl("https://twitter.com/vepo/not-a-status/1")).isEmpty();
        assertThat(plugin.render(List.of("https://github.com/vepo/repo"))).contains("content-render--error");
    }

    @Test
    void rendersEmbedHtmlFromOembed() {
        String embed = """
                       <blockquote class="twitter-tweet"><p>hello</p></blockquote>
                       <script async src="https://platform.twitter.com/widgets.js" charset="utf-8"></script>
                       """;
        server.createContext("/oembed", exchange -> {
            byte[] body = ("{\"html\":" + jsonString(embed) + "}").getBytes();
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        String url = "https://twitter.com/jack/status/20";
        assertThat(plugin.render(List.of(url)))
                                               .contains("content-render--twitter")
                                               .contains("twitter-tweet")
                                               .contains("platform.twitter.com/widgets.js");
    }

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/oembed");
        TwitterOEmbedClient client = new TwitterOEmbedClient(HttpClient.newHttpClient(), endpoint);
        plugin = new TwitterContentRenderPlugin(client);
    }

    @Test
    void showsLoadErrorWhenOembedFails() {
        server.createContext("/oembed", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        assertThat(plugin.render(List.of("https://twitter.com/jack/status/20"))).contains("Tweet could not be loaded.");
    }

    @Test
    void showsNotFoundWhenOembedReturns404() {
        server.createContext("/oembed", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        String url = "https://twitter.com/vepo/status/1787479135138230777";
        assertThat(plugin.render(List.of(url))).contains("Tweet could not be found.");
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }
}
