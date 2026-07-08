package dev.vepo.contraponto.activitypub.inbox;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URL;
import java.security.PrivateKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.activitypub.ActivityPubPaths;
import dev.vepo.contraponto.activitypub.actor.ActivityPubKeyPairService;
import dev.vepo.contraponto.activitypub.remote.ActivityPubRemoteActor;
import dev.vepo.contraponto.activitypub.remote.ActivityPubRemoteActorRepository;
import dev.vepo.contraponto.activitypub.security.ActivityPubHttpSignatureService;
import dev.vepo.contraponto.blog.BlogSubdomainConfig;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResource;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class ActivityPubInboxSecurityTest {

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

    private User user;
    private PrivateKey remotePrivateKey;
    private String remoteKeyId;

    private void configureRestAssured() {
        io.restassured.RestAssured.baseURI = baseUrl.getProtocol() + "://" + baseUrl.getHost();
        io.restassured.RestAssured.port = baseUrl.getPort();
        io.restassured.RestAssured.basePath = "";
    }

    private URI inboxUri() {
        return URI.create("%s://%s:%d/inboxsecuser/inbox".formatted(baseUrl.getProtocol(),
                                                                    baseUrl.getHost(),
                                                                    baseUrl.getPort()));
    }

    @Test
    void rejectsMismatchedActivityActorAndKeyId() {
        var actorId = ActivityPubPaths.actorId(user, subdomainConfig);
        var body = """
                   {
                     "@context": "https://www.w3.org/ns/activitystreams",
                     "id": "https://remote.example/follow/mismatch",
                     "type": "Follow",
                     "actor": "https://other.example/users/evil",
                     "object": "%s"
                   }
                   """.formatted(actorId);
        var signed = signatureService.signRequest(remotePrivateKey, remoteKeyId, "POST", inboxUri(), body);

        given().contentType(ActivityPubPaths.ACTIVITY_JSON)
               .headers(signed)
               .body(body)
               .post("/inboxsecuser/inbox")
               .then()
               .statusCode(401);
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
        configureRestAssured();
        user = Given.user()
                    .withUsername("inboxsecuser")
                    .withEmail("inboxsec@example.com")
                    .withPassword("pw123456789")
                    .withName("Inbox Security User")
                    .persist();
        Given.activityPubActor().withUser(user).persist();
        seedRemoteActorWithKeys();
    }
}
