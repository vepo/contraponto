package dev.vepo.contraponto.activitypub.inbox;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.vepo.contraponto.activitypub.ActivityPubActivityType;
import dev.vepo.contraponto.activitypub.ActivityPubFollowStatus;
import dev.vepo.contraponto.activitypub.ActivityPubPaths;
import dev.vepo.contraponto.activitypub.ActivityPubSettings;
import dev.vepo.contraponto.activitypub.actor.ActivityPubActor;
import dev.vepo.contraponto.activitypub.actor.ActivityPubActorService;
import dev.vepo.contraponto.activitypub.actor.ActivityPubActorUrls;
import dev.vepo.contraponto.activitypub.delivery.ActivityPubDeliveryService;
import dev.vepo.contraponto.activitypub.remote.ActivityPubRemoteActorRepository;
import dev.vepo.contraponto.activitypub.security.ActivityPubHttpSignatureService;
import dev.vepo.contraponto.blog.BlogSubdomainConfig;
import dev.vepo.contraponto.user.User;

@ApplicationScoped
public class ActivityPubInboxService {

    private static final Logger logger = LoggerFactory.getLogger(ActivityPubInboxService.class);

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final Pattern SIGNATURE_PARAM = Pattern.compile("(\\w+)=\"([^\"]*)\"");

    private final ActivityPubSettings settings;
    private final ActivityPubActorService actorService;
    private final ActivityPubFollowRepository followRepository;
    private final ActivityPubRemoteActorRepository remoteActorRepository;
    private final ActivityPubHttpSignatureService signatureService;
    private final ActivityPubDeliveryService deliveryService;
    private final ActivityPubFavouriteService favouriteService;
    private final BlogSubdomainConfig subdomainConfig;

    @Inject
    public ActivityPubInboxService(ActivityPubSettings settings,
                                   ActivityPubActorService actorService,
                                   ActivityPubFollowRepository followRepository,
                                   ActivityPubRemoteActorRepository remoteActorRepository,
                                   ActivityPubHttpSignatureService signatureService,
                                   ActivityPubDeliveryService deliveryService,
                                   ActivityPubFavouriteService favouriteService,
                                   BlogSubdomainConfig subdomainConfig) {
        this.settings = settings;
        this.actorService = actorService;
        this.followRepository = followRepository;
        this.remoteActorRepository = remoteActorRepository;
        this.signatureService = signatureService;
        this.deliveryService = deliveryService;
        this.favouriteService = favouriteService;
        this.subdomainConfig = subdomainConfig;
    }

    public void acceptPendingFollow(long followId) {
        var follow = followRepository.findById(followId)
                                     .orElseThrow(NotFoundException::new);
        if (follow.getStatus() != ActivityPubFollowStatus.PENDING) {
            return;
        }
        follow.accept();
        followRepository.update(follow);
        var acceptActivity = buildAcceptActivity(follow);
        deliveryService.enqueueToRemoteInbox(follow.getLocalActor(),
                                             ActivityPubActivityType.ACCEPT,
                                             follow.getFollowActivityId(),
                                             acceptActivity,
                                             follow.getRemoteActor().getInboxUrl());
        deliveryService.enqueueHistoricalPostsForAcceptedFollow(follow);
    }

    private void assertActivityActorMatchesKeyId(JsonNode root, Map<String, String> headers) {
        var signatureHeader = headerValue(headers, "Signature");
        if (signatureHeader == null || signatureHeader.isBlank()) {
            return;
        }
        var matcher = SIGNATURE_PARAM.matcher(signatureHeader);
        String keyId = null;
        while (matcher.find()) {
            if ("keyId".equals(matcher.group(1))) {
                keyId = matcher.group(2);
                break;
            }
        }
        if (keyId == null || keyId.isBlank()) {
            return;
        }
        var actorUrlFromKey = ActivityPubActorUrls.actorUrlFromKeyId(keyId);
        var activityActor = textValue(root, "actor");
        if (activityActor != null && !ActivityPubActorUrls.actorUrisMatch(activityActor, actorUrlFromKey)) {
            throw new NotAuthorizedException("Activity actor does not match signature keyId");
        }
    }

    private Map<String, Object> buildAcceptActivity(ActivityPubFollow follow) {
        User user = follow.getLocalActor().getUser();
        var activity = new LinkedHashMap<String, Object>();
        activity.put("@context", "https://www.w3.org/ns/activitystreams");
        activity.put("id", ActivityPubPaths.activityId(user, subdomainConfig, "accept", follow.getId()));
        activity.put("type", "Accept");
        activity.put("actor", ActivityPubPaths.actorId(user, subdomainConfig));
        activity.put("object", Map.of("id", follow.getFollowActivityId(),
                                      "type", "Follow",
                                      "actor", follow.getRemoteActor().getActorId(),
                                      "object", ActivityPubPaths.actorId(user, subdomainConfig)));
        return activity;
    }

    private void handleFollow(ActivityPubActor localActor, JsonNode root) {
        var actorUrl = textValue(root, "actor");
        var objectUrl = textValue(root, "object");
        var activityId = textValue(root, "id");
        if (actorUrl == null || objectUrl == null) {
            return;
        }
        var expectedActor = ActivityPubPaths.actorId(localActor.getUser(), subdomainConfig);
        if (!objectUrl.startsWith(expectedActor)) {
            return;
        }
        var remote = remoteActorRepository.findByActorId(ActivityPubActorUrls.normalizeActorUri(actorUrl))
                                          .orElse(null);
        if (remote == null) {
            logger.warn("Remote actor row missing after verify for actor={}", actorUrl);
            return;
        }
        var existing = followRepository.findByLocalAndRemote(localActor.getId(), remote.getId());
        if (existing.isPresent()) {
            var follow = existing.get();
            if (follow.getStatus() == ActivityPubFollowStatus.ACCEPTED
                    || follow.getStatus() == ActivityPubFollowStatus.PENDING) {
                return;
            }
            // REJECTED after Undo (unfollow): reopen and auto-accept again so
            // Accept + historical Create backfill run for the new Follow.
            follow.reopenAsPending(activityId);
            followRepository.update(follow);
            acceptPendingFollow(follow.getId());
            return;
        }
        var follow = followRepository.create(new ActivityPubFollow(localActor, remote, ActivityPubFollowStatus.PENDING, activityId));
        acceptPendingFollow(follow.getId());
    }

    public void handleInbox(String username, String body, Map<String, String> headers, URI requestUri) {
        if (!settings.enabled()) {
            throw new NotFoundException();
        }
        var actor = actorService.findEnabledByUsername(username)
                                .orElseThrow(NotFoundException::new);
        JsonNode root;
        try {
            root = JSON.readTree(body);
        } catch (Exception ex) {
            logger.warn("Invalid ActivityPub inbox payload for user {}", username);
            throw new NotAuthorizedException("Invalid activity payload");
        }
        if (ignoresInboxActivityWithoutLocalEffect(actor, root)) {
            logger.debug("Ignoring no-op inbox activity type={} for user={}", textValue(root, "type"), username);
            return;
        }
        verifySignature(body, headers, requestUri);
        assertActivityActorMatchesKeyId(root, headers);
        var type = textValue(root, "type");
        if ("Follow".equals(type)) {
            handleFollow(actor, root);
            return;
        }
        if ("Like".equals(type)) {
            handleLike(actor, root);
            return;
        }
        if ("Undo".equals(type)) {
            if (undoLikeObject(root) != null) {
                handleUndoLike(actor, root);
            } else {
                handleUndo(actor, root);
            }
            return;
        }
    }

    private void handleLike(ActivityPubActor localActor, JsonNode root) {
        var actorUrl = textValue(root, "actor");
        var objectUrl = objectUri(root);
        var likeActivityId = textValue(root, "id");
        if (actorUrl == null || objectUrl == null) {
            return;
        }
        var remote = remoteActorRepository.findByActorId(ActivityPubActorUrls.normalizeActorUri(actorUrl))
                                          .orElse(null);
        if (remote == null) {
            logger.warn("Remote actor row missing after verify for Like actor={}", actorUrl);
            return;
        }
        favouriteService.recordLike(localActor, remote, objectUrl, likeActivityId);
    }

    private void handleUndo(ActivityPubActor localActor, JsonNode root) {
        var objectNode = root.get("object");
        if (objectNode == null || !objectNode.isObject()) {
            return;
        }
        if (!"Follow".equals(textValue(objectNode, "type"))) {
            return;
        }
        var remoteActorUrl = textValue(objectNode, "actor");
        if (remoteActorUrl == null) {
            return;
        }
        var remote = remoteActorRepository.findByActorId(ActivityPubActorUrls.normalizeActorUri(remoteActorUrl))
                                          .orElse(null);
        if (remote == null) {
            return;
        }
        followRepository.findByLocalAndRemote(localActor.getId(), remote.getId())
                        .ifPresent(follow -> {
                            follow.reject();
                            followRepository.update(follow);
                        });
    }

    private void handleUndoLike(ActivityPubActor localActor, JsonNode root) {
        var likeObject = undoLikeObject(root);
        if (likeObject == null) {
            return;
        }
        var remoteActorUrl = textValue(likeObject, "actor");
        var objectUrl = objectUri(likeObject);
        if (remoteActorUrl == null || objectUrl == null) {
            return;
        }
        var remote = remoteActorRepository.findByActorId(ActivityPubActorUrls.normalizeActorUri(remoteActorUrl))
                                          .orElse(null);
        if (remote == null) {
            return;
        }
        favouriteService.removeLike(localActor, remote, objectUrl);
    }

    private String headerValue(Map<String, String> headers, String name) {
        for (var entry : headers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Delete and some Undo activities are ignored locally. Skipping HTTP signature
     * verification avoids refetching remote actor keys that often return HTTP 410
     * for deleted accounts when remotes clean up old deliveries.
     */
    private boolean ignoresInboxActivityWithoutLocalEffect(ActivityPubActor localActor, JsonNode root) {
        var type = textValue(root, "type");
        if ("Delete".equals(type)) {
            return true;
        }
        if ("Undo".equals(type)) {
            return !wouldUndoFollowChangeLocalState(localActor, root)
                    && !wouldUndoLikeChangeLocalState(localActor, root);
        }
        return false;
    }

    private String objectUri(JsonNode activity) {
        var objectNode = activity.get("object");
        if (objectNode == null) {
            return null;
        }
        if (objectNode.isTextual()) {
            return objectNode.asText();
        }
        if (objectNode.isObject()) {
            return textValue(objectNode, "id");
        }
        return null;
    }

    public void rejectPendingFollow(long followId) {
        var follow = followRepository.findById(followId)
                                     .orElseThrow(NotFoundException::new);
        if (follow.getStatus() != ActivityPubFollowStatus.PENDING) {
            return;
        }
        follow.reject();
        followRepository.update(follow);
    }

    private String textValue(JsonNode node, String field) {
        var value = node.get(field);
        return value != null && value.isTextual() ? value.asText() : null;
    }

    private JsonNode undoLikeObject(JsonNode root) {
        var objectNode = root.get("object");
        if (objectNode == null || !objectNode.isObject()) {
            return null;
        }
        if (!"Like".equals(textValue(objectNode, "type"))) {
            return null;
        }
        return objectNode;
    }

    private void verifySignature(String body, Map<String, String> headers, URI requestUri) {
        if (settings.insecureAcceptUnsigned()) {
            return;
        }
        if (!signatureService.verifyRequest("POST", requestUri, body, headers)) {
            throw new NotAuthorizedException("Invalid HTTP signature");
        }
    }

    private boolean wouldUndoFollowChangeLocalState(ActivityPubActor localActor, JsonNode root) {
        var objectNode = root.get("object");
        if (objectNode == null || !objectNode.isObject()) {
            return false;
        }
        if (!"Follow".equals(textValue(objectNode, "type"))) {
            return false;
        }
        var remoteActorUrl = textValue(objectNode, "actor");
        if (remoteActorUrl == null) {
            return false;
        }
        var remote = remoteActorRepository.findByActorId(ActivityPubActorUrls.normalizeActorUri(remoteActorUrl))
                                          .orElse(null);
        if (remote == null) {
            return false;
        }
        return followRepository.findByLocalAndRemote(localActor.getId(), remote.getId())
                               .map(follow -> follow.getStatus() != ActivityPubFollowStatus.REJECTED)
                               .orElse(false);
    }

    private boolean wouldUndoLikeChangeLocalState(ActivityPubActor localActor, JsonNode root) {
        var likeObject = undoLikeObject(root);
        if (likeObject == null) {
            return false;
        }
        var remoteActorUrl = textValue(likeObject, "actor");
        var objectUrl = objectUri(likeObject);
        if (remoteActorUrl == null || objectUrl == null) {
            return false;
        }
        var remote = remoteActorRepository.findByActorId(ActivityPubActorUrls.normalizeActorUri(remoteActorUrl))
                                          .orElse(null);
        if (remote == null) {
            return false;
        }
        return favouriteService.wouldRemoveFavourite(localActor, remote, objectUrl);
    }
}
