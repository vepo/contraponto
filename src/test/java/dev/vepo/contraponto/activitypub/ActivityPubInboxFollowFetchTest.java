package dev.vepo.contraponto.activitypub;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URL;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.BlogSubdomainConfig;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResource;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@QuarkusIntegrationTest
class ActivityPubInboxFollowFetchTest {

    private static final String KEYSTORE_RESOURCE = "/activitypub-test-https.p12";
    private static final char[] KEYSTORE_PASSWORD = "changeit".toCharArray();

    @TestHTTPResource("/")
    URL baseUrl;

    @Inject
    BlogSubdomainConfig subdomainConfig;

    @Inject
    ActivityPubKeyPairService keyPairService;

    @Inject
    ActivityPubHttpSignatureService signatureService;

    @Inject
    ActivityPubFollowRepository followRepository;

    @Inject
    ActivityPubRemoteActorRepository remoteActorRepository;

    private User user;
    private ActivityPubActor actor;

    private void configureRestAssured() {
        io.restassured.RestAssured.baseURI = baseUrl.getProtocol() + "://" + baseUrl.getHost();
        io.restassured.RestAssured.port = baseUrl.getPort();
        io.restassured.RestAssured.basePath = "";
    }

    private URI inboxUri() {
        return URI.create("%s://%s:%d/fetchfollowuser/inbox".formatted(baseUrl.getProtocol(),
                                                                       baseUrl.getHost(),
                                                                       baseUrl.getPort()));
    }

    private KeyPair loadTestKeyPair() throws Exception {
        try (var input = ActivityPubInboxFollowFetchTest.class.getResourceAsStream(KEYSTORE_RESOURCE)) {
            var keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(input, KEYSTORE_PASSWORD);
            var privateKey = (PrivateKey) keyStore.getKey("test", KEYSTORE_PASSWORD);
            var certificate = keyStore.getCertificate("test");
            return new KeyPair(certificate.getPublicKey(), privateKey);
        }
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        configureRestAssured();
        user = Given.user()
                    .withUsername("fetchfollowuser")
                    .withEmail("fetchfollow@example.com")
                    .withPassword("pw123456789")
                    .withName("Fetch Follow User")
                    .persist();
        actor = Given.activityPubActor().withUser(user).persist();
    }

    @Test
    @Transactional
    void signedFollowFetchesRemoteActorOnCacheMiss() throws Exception {
        var keyPair = loadTestKeyPair();
        var actorPath = "/users/reader";
        var publicKeyPem = keyPairService.toPublicKeyPem(keyPair.getPublic());
        var actorJsonTemplate = """
                                {
                                  "id": "%s",
                                  "type": "Person",
                                  "preferredUsername": "reader",
                                  "name": "Remote Reader",
                                  "inbox": "%s",
                                  "publicKey": {
                                    "id": "%s#main-key",
                                    "owner": "%s",
                                    "publicKeyPem": "%s"
                                  }
                                }
                                """;

        try (var actorServer = ActivityPubTestHttpsActorServer.start(actorPath, "")) {
            var remoteActorId = actorServer.actorUrl(actorPath);
            var inboxUrl = "https://127.0.0.1:%d/inbox".formatted(actorServer.port());
            var bodyJson = actorJsonTemplate.formatted(remoteActorId,
                                                       inboxUrl,
                                                       remoteActorId,
                                                       remoteActorId,
                                                       publicKeyPem.replace("\n", "\\n"));
            actorServer.updateResponse(bodyJson);

            var remoteKeyId = remoteActorId + "#main-key";
            var actorId = ActivityPubPaths.actorId(user, subdomainConfig);
            var followActivityId = remoteActorId + "/follow/1";
            var body = """
                       {
                         "@context": "https://www.w3.org/ns/activitystreams",
                         "id": "%s",
                         "type": "Follow",
                         "actor": "%s",
                         "object": "%s"
                       }
                       """.formatted(followActivityId, remoteActorId, actorId);
            var signed = signatureService.signRequest(keyPair.getPrivate(), remoteKeyId, "POST", inboxUri(), body);

            given().contentType(ActivityPubPaths.ACTIVITY_JSON)
                   .headers(signed)
                   .body(body)
                   .post("/fetchfollowuser/inbox")
                   .then()
                   .statusCode(202);
        }

        var pending = followRepository.listPendingByLocalActor(actor.getId());
        assertThat(pending).isEmpty();
        var accepted = followRepository.listAcceptedByLocalActor(actor.getId());
        assertThat(accepted).hasSize(1);
        var remote = remoteActorRepository.findByActorId(accepted.get(0).getRemoteActor().getActorId());
        assertThat(remote).isPresent();
        assertThat(remote.get().getInboxUrl()).contains("/inbox");
        assertThat(remote.get().getPublicKeyPem()).isNotBlank();
        assertThat(remote.get().getDisplayName()).isEqualTo("Remote Reader");
        assertThat(remote.get().getPreferredUsername()).isEqualTo("reader");
    }
}
