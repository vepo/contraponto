package dev.vepo.contraponto.content.render;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Fetches tweet embed HTML from Twitter's oEmbed endpoint.
 */
final class TwitterOEmbedClient {

    static final class TwitterOEmbedException extends RuntimeException {

        TwitterOEmbedException(String message) {
            super(message);
        }

        TwitterOEmbedException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static final URI OEMBED_ENDPOINT = URI.create("https://publish.twitter.com/oembed");

    private static final ObjectMapper JSON = new ObjectMapper();

    static TwitterOEmbedClient createDefault() {
        HttpClient client = HttpClient.newBuilder()
                                      .connectTimeout(Duration.ofSeconds(5))
                                      .followRedirects(HttpClient.Redirect.NORMAL)
                                      .build();
        return new TwitterOEmbedClient(client, OEMBED_ENDPOINT);
    }

    private final HttpClient httpClient;

    private final URI oembedEndpoint;

    TwitterOEmbedClient(HttpClient httpClient, URI oembedEndpoint) {
        this.httpClient = httpClient;
        this.oembedEndpoint = oembedEndpoint;
    }

    Optional<String> fetchEmbedHtml(String tweetUrl) {
        URI requestUri = URI.create("%s?url=%s".formatted(oembedEndpoint, URLEncoder.encode(tweetUrl, StandardCharsets.UTF_8)));
        HttpRequest request = HttpRequest.newBuilder(requestUri)
                                         .timeout(Duration.ofSeconds(10))
                                         .header("Accept", "application/json")
                                         .GET()
                                         .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                return Optional.empty();
            }
            if (response.statusCode() != 200) {
                throw new TwitterOEmbedException("Unexpected status %s".formatted(response.statusCode()));
            }
            JsonNode root = JSON.readTree(response.body());
            String html = root.path("html").asText(null);
            if (html == null || html.isBlank()) {
                throw new TwitterOEmbedException("Missing html in oEmbed response");
            }
            return Optional.of(html.trim());
        } catch (TwitterOEmbedException ex) {
            throw ex;
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            throw new TwitterOEmbedException("Failed to load tweet", ex);
        }
    }
}
