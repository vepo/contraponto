package dev.vepo.contraponto.activitypub;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.vepo.contraponto.blog.BlogSubdomainConfig;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ActivityPubDeliveryService {

    private static final Logger logger = LoggerFactory.getLogger(ActivityPubDeliveryService.class);

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(30);

    /**
     * {@link HttpClient} sets these from the request URI; including them in
     * {@link HttpRequest.Builder#header} throws {@link IllegalArgumentException}.
     */
    private static final Set<String> HTTP_CLIENT_RESTRICTED_HEADERS = Set.of("connection",
                                                                             "content-length",
                                                                             "expect",
                                                                             "host",
                                                                             "upgrade");

    static void applySignedHeaders(HttpRequest.Builder requestBuilder, Map<String, String> signedHeaders) {
        signedHeaders.forEach((name, value) -> {
            if (!HTTP_CLIENT_RESTRICTED_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
                requestBuilder.header(name, value);
            }
        });
    }

    private final ActivityPubSettings settings;
    private final ActivityPubDeliveryRepository deliveryRepository;
    private final ActivityPubFollowRepository followRepository;
    private final ActivityPubKeyPairService keyPairService;
    private final ActivityPubHttpSignatureService signatureService;
    private final ActivityPubPostObjectMapper postObjectMapper;
    private final PostRepository postRepository;
    private final BlogSubdomainConfig subdomainConfig;

    private final HttpClient httpClient;

    @Inject
    public ActivityPubDeliveryService(ActivityPubSettings settings,
                                      ActivityPubDeliveryRepository deliveryRepository,
                                      ActivityPubFollowRepository followRepository,
                                      ActivityPubKeyPairService keyPairService,
                                      ActivityPubHttpSignatureService signatureService,
                                      ActivityPubPostObjectMapper postObjectMapper,
                                      PostRepository postRepository,
                                      BlogSubdomainConfig subdomainConfig) {
        this(settings,
             deliveryRepository,
             followRepository,
             keyPairService,
             signatureService,
             postObjectMapper,
             postRepository,
             subdomainConfig,
             HttpClient.newBuilder().connectTimeout(HTTP_TIMEOUT).build());
    }

    ActivityPubDeliveryService(ActivityPubSettings settings,
                               ActivityPubDeliveryRepository deliveryRepository,
                               ActivityPubFollowRepository followRepository,
                               ActivityPubKeyPairService keyPairService,
                               ActivityPubHttpSignatureService signatureService,
                               ActivityPubPostObjectMapper postObjectMapper,
                               PostRepository postRepository,
                               BlogSubdomainConfig subdomainConfig,
                               HttpClient httpClient) {
        this.settings = settings;
        this.deliveryRepository = deliveryRepository;
        this.followRepository = followRepository;
        this.keyPairService = keyPairService;
        this.signatureService = signatureService;
        this.postObjectMapper = postObjectMapper;
        this.postRepository = postRepository;
        this.subdomainConfig = subdomainConfig;
        this.httpClient = httpClient;
    }

    private void deliverOne(ActivityPubDelivery delivery) {
        delivery.recordAttempt();
        try {
            var privateKey = keyPairService.decryptPrivateKey(delivery.getLocalActor().getPrivateKeyEncrypted());
            var target = URI.create(delivery.getTargetInboxUrl());
            var body = delivery.getPayloadJson();
            var signedHeaders = signatureService.signRequest(privateKey,
                                                             delivery.getLocalActor().getPublicKeyId(),
                                                             "POST",
                                                             target,
                                                             body);
            var requestBuilder = HttpRequest.newBuilder(target)
                                            .timeout(HTTP_TIMEOUT)
                                            .header("Content-Type", ActivityPubPaths.ACTIVITY_JSON)
                                            .POST(HttpRequest.BodyPublishers.ofString(body));
            applySignedHeaders(requestBuilder, signedHeaders);
            var response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                delivery.markDelivered();
            } else {
                delivery.markFailed("HTTP %d".formatted(response.statusCode()), nextRetry(delivery.getAttempts()));
            }
        } catch (Exception ex) {
            logger.warn("ActivityPub delivery failed id={}", delivery.getId(), ex);
            delivery.markFailed(ex.getMessage(), nextRetry(delivery.getAttempts()));
        }
        deliveryRepository.update(delivery);
    }

    public void enqueueCreateForPublishedPost(long postId, ActivityPubActor localActor) {
        if (!settings.enabled() || !localActor.isFederationEnabled()) {
            return;
        }
        var post = postRepository.findById(postId).orElse(null);
        if (post == null || !post.isPublished() || !post.getBlog().isMain()) {
            return;
        }
        var activity = postObjectMapper.toCreateActivity(post);
        fanOutToFollowers(localActor, ActivityPubActivityType.CREATE, ActivityPubPaths.postObjectId(post, subdomainConfig), activity);
    }

    public void enqueueDeleteForUnpublishedPost(long postId, ActivityPubActor localActor) {
        if (!settings.enabled() || !localActor.isFederationEnabled()) {
            return;
        }
        var post = postRepository.findById(postId).orElse(null);
        if (post == null || !post.getBlog().isMain()) {
            return;
        }
        var activity = postObjectMapper.toDeleteActivity(post);
        fanOutToFollowers(localActor, ActivityPubActivityType.DELETE, ActivityPubPaths.postObjectId(post, subdomainConfig), activity);
    }

    public void enqueueHistoricalPostsForAcceptedFollow(ActivityPubFollow follow) {
        if (!settings.enabled() || !follow.getLocalActor().isFederationEnabled()) {
            return;
        }
        if (follow.getStatus() != ActivityPubFollowStatus.ACCEPTED) {
            return;
        }
        var localActor = follow.getLocalActor();
        var inboxUrl = follow.getRemoteActor().getInboxUrl();
        for (var post : postRepository.findPublishedMainBlogByAuthorOldestFirst(localActor.getUser().getId())) {
            var activity = postObjectMapper.toCreateActivity(post);
            enqueueToRemoteInbox(localActor,
                                 ActivityPubActivityType.CREATE,
                                 ActivityPubPaths.postObjectId(post, subdomainConfig),
                                 activity,
                                 inboxUrl);
        }
    }

    public void enqueueToRemoteInbox(ActivityPubActor localActor,
                                     ActivityPubActivityType type,
                                     String objectId,
                                     Map<String, Object> activity,
                                     String inboxUrl) {
        if (!settings.enabled()) {
            return;
        }
        var payload = serialize(activity);
        deliveryRepository.create(new ActivityPubDelivery(localActor, type, objectId, payload, inboxUrl));
    }

    private void fanOutToFollowers(ActivityPubActor localActor,
                                   ActivityPubActivityType type,
                                   String objectId,
                                   Map<String, Object> activity) {
        var payload = serialize(activity);
        for (var follow : followRepository.listAcceptedByLocalActor(localActor.getId())) {
            deliveryRepository.create(new ActivityPubDelivery(localActor,
                                                              type,
                                                              objectId,
                                                              payload,
                                                              follow.getRemoteActor().getInboxUrl()));
        }
    }

    private LocalDateTime nextRetry(int attempts) {
        var delaySeconds = Math.min(3600, (long) Math.pow(2, attempts) * 30L);
        return LocalDateTime.now(ZoneId.systemDefault()).plusSeconds(delaySeconds);
    }

    public void processPendingDeliveries() {
        if (!settings.enabled()) {
            return;
        }
        var now = LocalDateTime.now(ZoneId.systemDefault());
        for (var delivery : deliveryRepository.findPendingReady(now)) {
            deliverOne(delivery);
        }
    }

    private String serialize(Map<String, Object> activity) {
        try {
            return JSON.writeValueAsString(activity);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize ActivityPub payload", ex);
        }
    }

}
