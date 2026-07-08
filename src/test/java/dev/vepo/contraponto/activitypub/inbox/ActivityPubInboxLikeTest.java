package dev.vepo.contraponto.activitypub.inbox;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URL;
import java.security.PrivateKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.activitypub.ActivityPubPaths;
import dev.vepo.contraponto.activitypub.actor.ActivityPubActor;
import dev.vepo.contraponto.activitypub.actor.ActivityPubKeyPairService;
import dev.vepo.contraponto.activitypub.remote.ActivityPubRemoteActor;
import dev.vepo.contraponto.activitypub.remote.ActivityPubRemoteActorRepository;
import dev.vepo.contraponto.activitypub.security.ActivityPubHttpSignatureService;
import dev.vepo.contraponto.blog.BlogSubdomainConfig;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResource;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@QuarkusIntegrationTest
class ActivityPubInboxLikeTest {

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
    ActivityPubFavouriteRepository favouriteRepository;

    @Inject
    EntityManager entityManager;

    private User user;
    private ActivityPubActor actor;
    private Post post;
    private PrivateKey remotePrivateKey;
    private String remoteKeyId;

    @Test
    void duplicateLikeIsIdempotent() {
        var objectUri = ActivityPubPaths.postObjectId(post, subdomainConfig);
        postSignedLike("https://remote.example/like/dup-1", objectUri);
        postSignedLike("https://remote.example/like/dup-2", objectUri);

        assertThat(favouriteRepository.countByPostId(post.getId())).isEqualTo(1);
    }

    private URI inboxUri() {
        return URI.create("%s://%s:%d/likeuser/inbox".formatted(baseUrl.getProtocol(),
                                                                baseUrl.getHost(),
                                                                baseUrl.getPort()));
    }

    private void postSignedLike(String likeActivityId, String objectUri) {
        var remoteActorId = "https://remote.example/users/liker";
        var body = """
                   {
                     "@context": "https://www.w3.org/ns/activitystreams",
                     "id": "%s",
                     "type": "Like",
                     "actor": "%s",
                     "object": "%s"
                   }
                   """.formatted(likeActivityId, remoteActorId, objectUri);
        var signed = signatureService.signRequest(remotePrivateKey, remoteKeyId, "POST", inboxUri(), body);
        given().contentType(ActivityPubPaths.ACTIVITY_JSON)
               .headers(signed)
               .body(body)
               .post("/likeuser/inbox")
               .then()
               .statusCode(202);
    }

    @Test
    void rejectsLikeWhenFederationOptInDisabled() {
        Given.transaction(() -> {
            actor.disableFederation();
            entityManager.merge(actor);
        });

        var objectUri = ActivityPubPaths.postObjectId(post, subdomainConfig);
        var body = """
                   {
                     "@context": "https://www.w3.org/ns/activitystreams",
                     "id": "https://remote.example/like/opt-out",
                     "type": "Like",
                     "actor": "https://remote.example/users/liker",
                     "object": "%s"
                   }
                   """.formatted(objectUri);
        var signed = signatureService.signRequest(remotePrivateKey, remoteKeyId, "POST", inboxUri(), body);
        given().contentType(ActivityPubPaths.ACTIVITY_JSON)
               .headers(signed)
               .body(body)
               .post("/likeuser/inbox")
               .then()
               .statusCode(404);
    }

    @Test
    void rejectsUnsignedLike() {
        var objectUri = ActivityPubPaths.postObjectId(post, subdomainConfig);
        var body = """
                   {
                     "@context": "https://www.w3.org/ns/activitystreams",
                     "id": "https://remote.example/like/unsigned",
                     "type": "Like",
                     "actor": "https://remote.example/users/liker",
                     "object": "%s"
                   }
                   """.formatted(objectUri);

        given().contentType(ActivityPubPaths.ACTIVITY_JSON)
               .body(body)
               .post("/likeuser/inbox")
               .then()
               .statusCode(401);
    }

    private void seedRemoteActorWithKeys() {
        Given.transaction(() -> {
            var remoteKeyPair = keyPairService.generateRsaKeyPair();
            var remoteActorId = "https://remote.example/users/liker";
            remoteKeyId = remoteActorId + "#mainKey";
            remotePrivateKey = remoteKeyPair.getPrivate();
            var remote = new ActivityPubRemoteActor(remoteActorId, "https://remote.example/inbox");
            remote.updatePublicKey(keyPairService.toPublicKeyPem(remoteKeyPair.getPublic()), remoteKeyId);
            remote.applyFetchedProfile("https://remote.example/inbox",
                                       remote.getPublicKeyPem(),
                                       remoteKeyId,
                                       "Liker",
                                       "liker");
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
                    .withUsername("likeuser")
                    .withEmail("likeuser@example.com")
                    .withPassword("pw123456789")
                    .withName("Like User")
                    .persist();
        actor = Given.activityPubActor().withUser(user).persist();
        post = Given.post()
                    .withAuthor(user)
                    .withTitle("Liked Post")
                    .withSlug("liked-post")
                    .withContent("Body")
                    .persist();
        seedRemoteActorWithKeys();
    }

    @Test
    void signedLikeCreatesFavourite() {
        var objectUri = ActivityPubPaths.postObjectId(post, subdomainConfig);
        postSignedLike("https://remote.example/like/1", objectUri);

        assertThat(favouriteRepository.countByPostId(post.getId())).isEqualTo(1);
    }

    @Test
    @Transactional
    void undoLikeRemovesFavourite() {
        var objectUri = ActivityPubPaths.postObjectId(post, subdomainConfig);
        postSignedLike("https://remote.example/like/remove", objectUri);
        assertThat(favouriteRepository.countByPostId(post.getId())).isEqualTo(1);

        var remoteActorId = "https://remote.example/users/liker";
        var undoBody = """
                       {
                         "@context": "https://www.w3.org/ns/activitystreams",
                         "id": "https://remote.example/undo/like/1",
                         "type": "Undo",
                         "actor": "%s",
                         "object": {
                           "id": "https://remote.example/like/remove",
                           "type": "Like",
                           "actor": "%s",
                           "object": "%s"
                         }
                       }
                       """.formatted(remoteActorId, remoteActorId, objectUri);
        var signed = signatureService.signRequest(remotePrivateKey, remoteKeyId, "POST", inboxUri(), undoBody);
        given().contentType(ActivityPubPaths.ACTIVITY_JSON)
               .headers(signed)
               .body(undoBody)
               .post("/likeuser/inbox")
               .then()
               .statusCode(202);

        entityManager.clear();
        assertThat(favouriteRepository.countByPostId(post.getId())).isZero();
    }
}
