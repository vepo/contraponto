package dev.vepo.contraponto.activitypub;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.vepo.contraponto.blog.BlogSubdomainConfig;
import dev.vepo.contraponto.user.User;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class ActivityPubInboxService {

    private static final Logger logger = LoggerFactory.getLogger(ActivityPubInboxService.class);

    private static final ObjectMapper JSON = new ObjectMapper();

    private final ActivityPubSettings settings;
    private final ActivityPubActorService actorService;
    private final ActivityPubFollowRepository followRepository;
    private final ActivityPubRemoteActorRepository remoteActorRepository;
    private final ActivityPubHttpSignatureService signatureService;
    private final ActivityPubDeliveryService deliveryService;
    private final BlogSubdomainConfig subdomainConfig;

    @Inject
    public ActivityPubInboxService(ActivityPubSettings settings,
                                   ActivityPubActorService actorService,
                                   ActivityPubFollowRepository followRepository,
                                   ActivityPubRemoteActorRepository remoteActorRepository,
                                   ActivityPubHttpSignatureService signatureService,
                                   ActivityPubDeliveryService deliveryService,
                                   BlogSubdomainConfig subdomainConfig) {
        this.settings = settings;
        this.actorService = actorService;
        this.followRepository = followRepository;
        this.remoteActorRepository = remoteActorRepository;
        this.signatureService = signatureService;
        this.deliveryService = deliveryService;
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
        var remote = remoteActorRepository.findByActorId(actorUrl)
                                          .orElseGet(() -> remoteActorRepository.create(new ActivityPubRemoteActor(actorUrl, actorUrl)));
        var existing = followRepository.findByLocalAndRemote(localActor.getId(), remote.getId());
        if (existing.isPresent()) {
            return;
        }
        followRepository.create(new ActivityPubFollow(localActor, remote, ActivityPubFollowStatus.PENDING, activityId));
    }

    public void handleInbox(String username, String body, Map<String, String> headers, URI requestUri) {
        if (!settings.enabled()) {
            throw new NotFoundException();
        }
        var actor = actorService.findEnabledByUsername(username)
                                .orElseThrow(NotFoundException::new);
        verifySignature(body, headers, requestUri);
        JsonNode root;
        try {
            root = JSON.readTree(body);
        } catch (Exception ex) {
            logger.warn("Invalid ActivityPub inbox payload for user {}", username);
            throw new NotAuthorizedException("Invalid activity payload");
        }
        var type = textValue(root, "type");
        if ("Follow".equals(type)) {
            handleFollow(actor, root);
            return;
        }
        if ("Undo".equals(type)) {
            handleUndo(actor, root);
        }
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
        var remote = remoteActorRepository.findByActorId(remoteActorUrl).orElse(null);
        if (remote == null) {
            return;
        }
        followRepository.findByLocalAndRemote(localActor.getId(), remote.getId())
                        .ifPresent(follow -> {
                            follow.reject();
                            followRepository.update(follow);
                        });
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

    private void verifySignature(String body, Map<String, String> headers, URI requestUri) {
        if (settings.insecureAcceptUnsigned()) {
            return;
        }
        if (!signatureService.verifyRequest("POST", requestUri, body, headers)) {
            throw new NotAuthorizedException("Invalid HTTP signature");
        }
    }
}
