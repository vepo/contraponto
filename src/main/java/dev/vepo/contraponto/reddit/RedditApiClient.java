package dev.vepo.contraponto.reddit;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Thin wrapper around the Reddit OAuth + REST API. Kept intentionally small and
 * side-effect free (no persistence) so callers control transactions and tests
 * can mock it directly.
 */
@ApplicationScoped
public class RedditApiClient {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(30);
    private static final String ACCESS_TOKEN_URL = "https://www.reddit.com/api/v1/access_token";
    private static final String REVOKE_TOKEN_URL = "https://www.reddit.com/api/v1/revoke_token";
    private static final String ME_URL = "https://oauth.reddit.com/api/v1/me";
    private static final String SUBMIT_URL = "https://oauth.reddit.com/api/submit";
    private static final String DEL_URL = "https://oauth.reddit.com/api/del";

    private final RedditSettings settings;
    private final HttpClient httpClient;

    @Inject
    public RedditApiClient(RedditSettings settings) {
        this(settings, HttpClient.newBuilder().connectTimeout(HTTP_TIMEOUT).build());
    }

    RedditApiClient(RedditSettings settings, HttpClient httpClient) {
        this.settings = settings;
        this.httpClient = httpClient;
    }

    private String basicAuthHeader() {
        var clientId = settings.clientId().orElseThrow(() -> new RedditApiException("Reddit client id is not configured"));
        var clientSecret = settings.clientSecret().orElseThrow(() -> new RedditApiException("Reddit client secret is not configured"));
        var credentials = "%s:%s".formatted(clientId, clientSecret);
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    public void deleteSubmission(String accessToken, String fullname) {
        var response = post(DEL_URL, Map.of("id", fullname), "Bearer " + accessToken);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RedditApiException("Reddit delete failed: HTTP %d".formatted(response.statusCode()));
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public RedditTokenResponse exchangeAuthorizationCode(String code) {
        var redirectUri = settings.redirectUri().orElseThrow(() -> new RedditApiException("Reddit redirect URI is not configured"));
        var form = new LinkedHashMap<String, String>();
        form.put("grant_type", "authorization_code");
        form.put("code", code);
        form.put("redirect_uri", redirectUri);
        return requestToken(form);
    }

    public String fetchUsername(String accessToken) {
        var response = get(ME_URL, "Bearer " + accessToken);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RedditApiException("Unable to resolve Reddit username: HTTP %d".formatted(response.statusCode()));
        }
        try {
            var node = JSON.readTree(response.body());
            var name = node.path("name").asText(null);
            if (name == null || name.isBlank()) {
                throw new RedditApiException("Reddit /api/v1/me response missing username");
            }
            return name;
        } catch (Exception ex) {
            throw new RedditApiException("Unable to parse Reddit /api/v1/me response", ex);
        }
    }

    private HttpResponse<String> get(String url, String authorizationHeader) {
        try {
            var request = HttpRequest.newBuilder(URI.create(url))
                                     .timeout(HTTP_TIMEOUT)
                                     .header("User-Agent", settings.userAgent())
                                     .header("Authorization", authorizationHeader)
                                     .GET()
                                     .build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception ex) {
            throw new RedditApiException("Reddit GET %s failed".formatted(url), ex);
        }
    }

    private RedditSubmissionResult parseSubmission(String body) {
        try {
            JsonNode json = JSON.readTree(body).path("json");
            var errors = json.path("errors");
            if (errors.isArray() && !errors.isEmpty()) {
                throw new RedditApiException("Reddit submit rejected: %s".formatted(errors.toString()));
            }
            var data = json.path("data");
            var fullname = data.path("name").asText(null);
            var permalink = data.path("url").asText(null);
            if (fullname == null || fullname.isBlank()) {
                throw new RedditApiException("Reddit submit response missing submission name");
            }
            return new RedditSubmissionResult(fullname, permalink);
        } catch (RedditApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RedditApiException("Unable to parse Reddit submit response", ex);
        }
    }

    private HttpResponse<String> post(String url, Map<String, String> form, String authorizationHeader) {
        var body = form.entrySet()
                       .stream()
                       .map(entry -> "%s=%s".formatted(encode(entry.getKey()), encode(entry.getValue())))
                       .collect(Collectors.joining("&"));
        try {
            var request = HttpRequest.newBuilder(URI.create(url))
                                     .timeout(HTTP_TIMEOUT)
                                     .header("User-Agent", settings.userAgent())
                                     .header("Authorization", authorizationHeader)
                                     .header("Content-Type", "application/x-www-form-urlencoded")
                                     .POST(BodyPublishers.ofString(body))
                                     .build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception ex) {
            throw new RedditApiException("Reddit POST %s failed".formatted(url), ex);
        }
    }

    public RedditTokenResponse refreshAccessToken(String refreshToken) {
        var form = new LinkedHashMap<String, String>();
        form.put("grant_type", "refresh_token");
        form.put("refresh_token", refreshToken);
        return requestToken(form);
    }

    private RedditTokenResponse requestToken(Map<String, String> form) {
        var response = post(ACCESS_TOKEN_URL, form, basicAuthHeader());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RedditApiException("Reddit token request failed: HTTP %d".formatted(response.statusCode()));
        }
        try {
            var node = JSON.readTree(response.body());
            var accessToken = node.path("access_token").asText(null);
            if (accessToken == null || accessToken.isBlank()) {
                throw new RedditApiException("Reddit token response missing access_token");
            }
            // Absent on refresh-token grants: Reddit does not always reissue a new refresh
            // token.
            var refreshToken = node.path("refresh_token").asText(null);
            return new RedditTokenResponse(accessToken, refreshToken);
        } catch (RedditApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RedditApiException("Unable to parse Reddit token response", ex);
        }
    }

    public void revokeToken(String refreshToken) {
        var form = Map.of("token", refreshToken, "token_type_hint", "refresh_token");
        var response = post(REVOKE_TOKEN_URL, form, basicAuthHeader());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RedditApiException("Reddit revoke failed: HTTP %d".formatted(response.statusCode()));
        }
    }

    public RedditSubmissionResult submitLinkPost(String accessToken, String subreddit, String title, String url) {
        var form = new LinkedHashMap<String, String>();
        form.put("sr", subreddit);
        form.put("kind", "link");
        form.put("title", title);
        form.put("url", url);
        form.put("api_type", "json");
        form.put("resubmit", "true");
        var response = post(SUBMIT_URL, form, "Bearer " + accessToken);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RedditApiException("Reddit submit failed: HTTP %d".formatted(response.statusCode()));
        }
        return parseSubmission(response.body());
    }
}
