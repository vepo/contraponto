package dev.vepo.contraponto.activitypub;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URL;
import java.security.PrivateKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.BlogSubdomainConfig;
import dev.vepo.contraponto.notification.PostPublishedEvent;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResource;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@QuarkusIntegrationTest
class ActivityPubInboxFollowTest {

    private static final class RestAssuredPort {
        static void configure(URL baseUrl) {
            io.restassured.RestAssured.baseURI = baseUrl.getProtocol() + "://" + baseUrl.getHost();
            io.restassured.RestAssured.port = baseUrl.getPort();
            io.restassured.RestAssured.basePath = "";
        }

        private RestAssuredPort() {}
    }

    @TestHTTPResource("/")
    URL baseUrl;

    @Inject
    BlogSubdomainConfig subdomainConfig;

    @Inject
    ActivityPubKeyPairService keyPairService;

    @Inject
    ActivityPubHttpSignatureService signatureService;

    @Inject
    ActivityPubRemoteActorRepository remoteActorRepository;

    @Inject
    ActivityPubFollowRepository followRepository;

    @Inject
    ActivityPubInboxService inboxService;

    @Inject
    ActivityPubDeliveryObserver deliveryObserver;

    @Inject
    ActivityPubDeliveryRepository deliveryRepository;

    private User user;
    private ActivityPubActor actor;
    private PrivateKey remotePrivateKey;
    private String remoteKeyId;

    @Test
    @Transactional
    void acceptFollowBackfillsHistoricalMainBlogPosts() {
        Given.post()
             .withAuthor(user)
             .withTitle("Historical One")
             .withSlug("historical-one")
             .withDescription("Summary")
             .withContent("Body")
             .persist();
        Given.post()
             .withAuthor(user)
             .withTitle("Historical Two")
             .withSlug("historical-two")
             .withDescription("Summary")
             .withContent("Body")
             .persist();

        var actorId = ActivityPubPaths.actorId(user, subdomainConfig);
        var remoteActorId = "https://remote.example/users/reader";
        var followActivityId = "https://remote.example/follow/backfill";
        var body = """
                   {
                     "@context": "https://www.w3.org/ns/activitystreams",
                     "id": "%s",
                     "type": "Follow",
                     "actor": "%s",
                     "object": "%s"
                   }
                   """.formatted(followActivityId, remoteActorId, actorId);
        var target = inboxUri();
        var signed = signatureService.signRequest(remotePrivateKey, remoteKeyId, "POST", target, body);

        given().contentType(ActivityPubPaths.ACTIVITY_JSON)
               .headers(signed)
               .body(body)
               .post("/followuser/inbox")
               .then()
               .statusCode(202);

        var pending = followRepository.listPendingByLocalActor(actor.getId());
        inboxService.acceptPendingFollow(pending.get(0).getId());

        var pendingDeliveries = deliveryRepository.findPendingReady(java.time.LocalDateTime.now().plusMinutes(1));
        var createDeliveries = pendingDeliveries.stream()
                                                .filter(d -> d.getActivityType() == ActivityPubActivityType.CREATE)
                                                .toList();
        assertThat(createDeliveries).hasSize(2);
        assertThat(createDeliveries.get(0).getPayloadJson()).contains("Historical One");
        assertThat(createDeliveries.get(1).getPayloadJson()).contains("Historical Two");
    }

    private URI inboxUri() {
        return URI.create("%s://%s:%d/followuser/inbox".formatted(baseUrl.getProtocol(),
                                                                  baseUrl.getHost(),
                                                                  baseUrl.getPort()));
    }

    private void seedRemoteActorWithKeys() {
        Given.transaction(() -> {
            var remoteKeyPair = keyPairService.generateRsaKeyPair();
            var remoteActorId = "https://remote.example/users/reader";
            remoteKeyId = remoteActorId + "#mainKey";
            remotePrivateKey = remoteKeyPair.getPrivate();
            var remote = new ActivityPubRemoteActor(remoteActorId, "https://remote.example/inbox");
            remote.updatePublicKey(keyPairService.toPublicKeyPem(remoteKeyPair.getPublic()), remoteKeyId);
            remoteActorRepository.create(remote);
        });
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        RestAssuredPort.configure(baseUrl);
        user = Given.user()
                    .withUsername("followuser")
                    .withEmail("followuser@example.com")
                    .withPassword("pw123456789")
                    .withName("Follow User")
                    .persist();
        actor = Given.activityPubActor().withUser(user).persist();
        seedRemoteActorWithKeys();
    }

    @Test
    @Transactional
    void signedFollowCreatesPendingRequestAcceptEnqueuesCreateOnPublish() {
        var actorId = ActivityPubPaths.actorId(user, subdomainConfig);
        var remoteActorId = "https://remote.example/users/reader";
        var followActivityId = "https://remote.example/follow/1";
        var body = """
                   {
                     "@context": "https://www.w3.org/ns/activitystreams",
                     "id": "%s",
                     "type": "Follow",
                     "actor": "%s",
                     "object": "%s"
                   }
                   """.formatted(followActivityId, remoteActorId, actorId);
        var target = inboxUri();
        var signed = signatureService.signRequest(remotePrivateKey, remoteKeyId, "POST", target, body);

        given().contentType(ActivityPubPaths.ACTIVITY_JSON)
               .headers(signed)
               .body(body)
               .post("/followuser/inbox")
               .then()
               .statusCode(202);

        var pending = followRepository.listPendingByLocalActor(actor.getId());
        assertThat(pending).hasSize(1);

        inboxService.acceptPendingFollow(pending.get(0).getId());
        assertThat(followRepository.listAcceptedByLocalActor(actor.getId())).hasSize(1);

        var post = Given.post()
                        .withAuthor(user)
                        .withTitle("Fediverse Delivery")
                        .withSlug("fediverse-delivery")
                        .withDescription("Summary")
                        .withContent("Body")
                        .persist();
        deliveryObserver.afterPublish(new PostPublishedEvent(post.getId(),
                                                             post.getLivePublication().getId(),
                                                             post.getBlog().getId(),
                                                             user.getId()));
        var pendingDeliveries = deliveryRepository.findPendingReady(java.time.LocalDateTime.now().plusMinutes(1));
        assertThat(pendingDeliveries.stream().filter(d -> d.getActivityType() == ActivityPubActivityType.CREATE)).hasSize(1);
    }
}
