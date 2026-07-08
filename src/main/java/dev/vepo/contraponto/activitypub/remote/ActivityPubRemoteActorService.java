package dev.vepo.contraponto.activitypub.remote;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.Optional;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.vepo.contraponto.activitypub.ActivityPubPaths;
import dev.vepo.contraponto.activitypub.actor.ActivityPubActorUrls;
import dev.vepo.contraponto.shared.security.OutboundHttpsUrlValidator;

@ApplicationScoped
public class ActivityPubRemoteActorService {

    private static final Logger logger = LoggerFactory.getLogger(ActivityPubRemoteActorService.class);

    private static final ObjectMapper JSON = new ObjectMapper();

    private static HttpClient buildHttpClient(ActivityPubFetchSettings fetchSettings, boolean trustAllTls) {
        var builder = HttpClient.newBuilder().connectTimeout(fetchSettings.connectTimeout());
        if (trustAllTls) {
            builder.sslContext(trustAllSslContext());
        }
        return builder.build();
    }

    private static SSLContext trustAllSslContext() {
        try {
            var context = SSLContext.getInstance("TLS");
            context.init(null,
                         new TrustManager[] { new X509TrustManager() {
                             @Override
                             public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}

                             @Override
                             public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}

                             @Override
                             public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                 return new java.security.cert.X509Certificate[0];
                             }
                         } },
                         new java.security.SecureRandom());
            return context;
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to build test SSL context", ex);
        }
    }

    private final ActivityPubFetchSettings fetchSettings;
    private final ActivityPubRemoteActorRepository remoteActorRepository;
    private final OutboundHttpsUrlValidator outboundHttpsUrlValidator;

    private final ActivityPubFetchRateLimiter rateLimiter;

    private final HttpClient httpClient;

    @Inject
    public ActivityPubRemoteActorService(ActivityPubFetchSettings fetchSettings,
                                         ActivityPubRemoteActorRepository remoteActorRepository,
                                         OutboundHttpsUrlValidator outboundHttpsUrlValidator,
                                         ActivityPubFetchRateLimiter rateLimiter,
                                         @ConfigProperty(name = "contraponto.activitypub.fetch.trust-all-tls", defaultValue = "false") boolean trustAllTls) {
        this(fetchSettings,
             remoteActorRepository,
             outboundHttpsUrlValidator,
             rateLimiter,
             buildHttpClient(fetchSettings, trustAllTls));
    }

    ActivityPubRemoteActorService(ActivityPubFetchSettings fetchSettings,
                                  ActivityPubRemoteActorRepository remoteActorRepository,
                                  OutboundHttpsUrlValidator outboundHttpsUrlValidator,
                                  ActivityPubFetchRateLimiter rateLimiter,
                                  HttpClient httpClient) {
        this.fetchSettings = fetchSettings;
        this.remoteActorRepository = remoteActorRepository;
        this.outboundHttpsUrlValidator = outboundHttpsUrlValidator;
        this.rateLimiter = rateLimiter;
        this.httpClient = httpClient;
    }

    private Optional<String> fetchActorDocument(String actorUrl, String domain) {
        try {
            var request = HttpRequest.newBuilder(URI.create(actorUrl))
                                     .timeout(fetchSettings.requestTimeout())
                                     .header("Accept", ActivityPubPaths.ACTIVITY_JSON)
                                     .GET()
                                     .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                logger.warn("Remote actor fetch HTTP {} for actorUrl={} host={}",
                            response.statusCode(),
                            actorUrl,
                            domain);
                return Optional.empty();
            }
            return Optional.ofNullable(response.body());
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            logger.warn("Remote actor fetch failed for actorUrl={} host={}: {}",
                        actorUrl,
                        domain,
                        ex.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    @Transactional
    public Optional<ActivityPubRemoteActor> fetchAndCache(String actorUrl) {
        return fetchAndCache(actorUrl, false);
    }

    @Transactional
    public Optional<ActivityPubRemoteActor> fetchAndCache(String actorUrl, boolean forceRefetch) {
        var normalizedActorUrl = ActivityPubActorUrls.normalizeActorUri(actorUrl);
        if (normalizedActorUrl.isBlank()) {
            logger.warn("Remote actor fetch skipped: blank actor URL");
            return Optional.empty();
        }
        var validationError = outboundHttpsUrlValidator.validateHttpsUrl(normalizedActorUrl);
        if (validationError.isPresent()) {
            logger.warn("Remote actor fetch blocked for actorUrl={}: {}", normalizedActorUrl, validationError.get());
            return Optional.empty();
        }
        var cached = remoteActorRepository.findByActorId(normalizedActorUrl);
        if (!forceRefetch && cached.isPresent() && hasFreshProfile(cached.get())) {
            return cached;
        }
        var domain = URI.create(normalizedActorUrl).getHost();
        if (domain == null || domain.isBlank() || !rateLimiter.tryAcquire(domain)) {
            logger.warn("Remote actor fetch rate limited for actorUrl={} host={}", normalizedActorUrl, domain);
            return Optional.empty();
        }
        var body = fetchActorDocument(normalizedActorUrl, domain);
        if (body.isEmpty()) {
            return Optional.empty();
        }
        return parseAndUpsert(normalizedActorUrl, body.get());
    }

    private boolean hasFreshProfile(ActivityPubRemoteActor remote) {
        if (remote.getPublicKeyPem() == null || remote.getPublicKeyPem().isBlank()) {
            return false;
        }
        if (remote.getInboxUrl() == null || remote.getInboxUrl().isBlank()) {
            return false;
        }
        var fetchedAt = remote.getProfileFetchedAt();
        if (fetchedAt == null) {
            return false;
        }
        return fetchedAt.isAfter(LocalDateTime.now().minusDays(fetchSettings.profileMaxAgeDays()));
    }

    Optional<ActivityPubRemoteActor> parseAndUpsert(String actorUrl, String body) {
        JsonNode root;
        try {
            root = JSON.readTree(body);
        } catch (Exception ex) {
            logger.warn("Remote actor fetch invalid JSON for actorUrl={}", actorUrl);
            return Optional.empty();
        }
        var inboxUrl = textValue(root, "inbox");
        if (inboxUrl == null || inboxUrl.isBlank()) {
            logger.warn("Remote actor fetch missing inbox for actorUrl={}", actorUrl);
            return Optional.empty();
        }
        var publicKeyNode = root.get("publicKey");
        if (publicKeyNode == null || !publicKeyNode.isObject()) {
            logger.warn("Remote actor fetch missing publicKey for actorUrl={}", actorUrl);
            return Optional.empty();
        }
        var publicKeyPem = textValue(publicKeyNode, "publicKeyPem");
        var publicKeyId = textValue(publicKeyNode, "id");
        if (publicKeyPem == null || publicKeyPem.isBlank() || publicKeyId == null || publicKeyId.isBlank()) {
            logger.warn("Remote actor fetch missing publicKey material for actorUrl={}", actorUrl);
            return Optional.empty();
        }
        var displayName = textValue(root, "name");
        var preferredUsername = textValue(root, "preferredUsername");
        var remote = remoteActorRepository.findByActorId(actorUrl)
                                          .orElseGet(() -> new ActivityPubRemoteActor(actorUrl, inboxUrl));
        remote.applyFetchedProfile(inboxUrl, publicKeyPem, publicKeyId, displayName, preferredUsername);
        return Optional.of(remoteActorRepository.save(remote));
    }

    private String textValue(JsonNode node, String field) {
        var value = node.get(field);
        return value != null && value.isTextual() ? value.asText() : null;
    }
}
