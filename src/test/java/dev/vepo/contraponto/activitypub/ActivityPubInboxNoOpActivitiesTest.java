package dev.vepo.contraponto.activitypub;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.BlogSubdomainConfig;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResource;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class ActivityPubInboxNoOpActivitiesTest {

    @TestHTTPResource("/")
    URL baseUrl;

    @Inject
    BlogSubdomainConfig subdomainConfig;

    @Inject
    ActivityPubKeyPairService keyPairService;

    @Inject
    ActivityPubRemoteActorRepository remoteActorRepository;

    @Inject
    ActivityPubFollowRepository followRepository;

    private User user;
    private ActivityPubActor actor;

    @Test
    void acceptsUnsignedDeleteWithoutFetchingRemoteActorKey() {
        var body = """
                   {
                     "@context": "https://www.w3.org/ns/activitystreams",
                     "id": "https://gone.remote.example/delete/status/1",
                     "type": "Delete",
                     "actor": "https://gone.remote.example/users/deleted",
                     "object": "https://gone.remote.example/statuses/1"
                   }
                   """;

        given().contentType(ActivityPubPaths.ACTIVITY_JSON)
               .body(body)
               .post("/noopinboxuser/inbox")
               .then()
               .statusCode(202);
    }

    @Test
    void acceptsUnsignedUndoWhenFollowAlreadyRejected() {
        var actorId = ActivityPubPaths.actorId(user, subdomainConfig);
        var remoteActorId = "https://remote.example/users/reader";
        Given.transaction(() -> {
            var remote = remoteActorRepository.findByActorId(remoteActorId).orElseThrow();
            var follow = new ActivityPubFollow(actor, remote, ActivityPubFollowStatus.REJECTED, "https://remote.example/follow/old");
            followRepository.create(follow);
        });

        var body = """
                   {
                     "@context": "https://www.w3.org/ns/activitystreams",
                     "id": "https://remote.example/undo/follow/repeat",
                     "type": "Undo",
                     "actor": "%s",
                     "object": {
                       "id": "https://remote.example/follow/old",
                       "type": "Follow",
                       "actor": "%s",
                       "object": "%s"
                     }
                   }
                   """.formatted(remoteActorId, remoteActorId, actorId);

        given().contentType(ActivityPubPaths.ACTIVITY_JSON)
               .body(body)
               .post("/noopinboxuser/inbox")
               .then()
               .statusCode(202);

        var remote = remoteActorRepository.findByActorId(remoteActorId).orElseThrow();
        var follow = followRepository.findByLocalAndRemote(actor.getId(), remote.getId()).orElseThrow();
        assertThat(follow.getStatus()).isEqualTo(ActivityPubFollowStatus.REJECTED);
    }

    @Test
    void acceptsUnsignedUndoWhenNoFollowEdgeExists() {
        var actorId = ActivityPubPaths.actorId(user, subdomainConfig);
        var body = """
                   {
                     "@context": "https://www.w3.org/ns/activitystreams",
                     "id": "https://gone.remote.example/undo/follow/1",
                     "type": "Undo",
                     "actor": "https://gone.remote.example/users/deleted",
                     "object": {
                       "id": "https://gone.remote.example/follow/1",
                       "type": "Follow",
                       "actor": "https://gone.remote.example/users/deleted",
                       "object": "%s"
                     }
                   }
                   """.formatted(actorId);

        given().contentType(ActivityPubPaths.ACTIVITY_JSON)
               .body(body)
               .post("/noopinboxuser/inbox")
               .then()
               .statusCode(202);
    }

    @Test
    void rejectsUnsignedUndoWhenFollowWouldBeRejected() {
        var actorId = ActivityPubPaths.actorId(user, subdomainConfig);
        var remoteActorId = "https://remote.example/users/reader";
        Given.transaction(() -> {
            var remote = remoteActorRepository.findByActorId(remoteActorId).orElseThrow();
            var follow = new ActivityPubFollow(actor, remote, ActivityPubFollowStatus.ACCEPTED, "https://remote.example/follow/active");
            followRepository.create(follow);
        });

        var body = """
                   {
                     "@context": "https://www.w3.org/ns/activitystreams",
                     "id": "https://remote.example/undo/follow/active",
                     "type": "Undo",
                     "actor": "%s",
                     "object": {
                       "id": "https://remote.example/follow/active",
                       "type": "Follow",
                       "actor": "%s",
                       "object": "%s"
                     }
                   }
                   """.formatted(remoteActorId, remoteActorId, actorId);

        given().contentType(ActivityPubPaths.ACTIVITY_JSON)
               .body(body)
               .post("/noopinboxuser/inbox")
               .then()
               .statusCode(401);
    }

    private void seedRemoteActorWithKeys() {
        Given.transaction(() -> {
            var remoteKeyPair = keyPairService.generateRsaKeyPair();
            var remoteActorId = "https://remote.example/users/reader";
            var remoteKeyId = remoteActorId + "#mainKey";
            var remote = new ActivityPubRemoteActor(remoteActorId, "https://remote.example/inbox");
            remote.updatePublicKey(keyPairService.toPublicKeyPem(remoteKeyPair.getPublic()), remoteKeyId);
            remoteActorRepository.create(remote);
        });
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        io.restassured.RestAssured.baseURI = baseUrl.getProtocol() + "://" + baseUrl.getHost();
        io.restassured.RestAssured.port = baseUrl.getPort();
        io.restassured.RestAssured.basePath = "";
        user = Given.user()
                    .withUsername("noopinboxuser")
                    .withEmail("noopinbox@example.com")
                    .withPassword("pw123456789")
                    .withName("No-op Inbox User")
                    .persist();
        actor = Given.activityPubActor().withUser(user).persist();
        seedRemoteActorWithKeys();
    }
}
