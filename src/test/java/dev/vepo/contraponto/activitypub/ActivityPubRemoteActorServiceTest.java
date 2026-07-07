package dev.vepo.contraponto.activitypub;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@QuarkusIntegrationTest
class ActivityPubRemoteActorServiceTest {

    @Inject
    ActivityPubRemoteActorService remoteActorService;

    @Inject
    ActivityPubKeyPairService keyPairService;

    @Inject
    ActivityPubRemoteActorRepository remoteActorRepository;

    @Test
    @Transactional
    void entityMapsDisplayColumns() {
        var remote = new ActivityPubRemoteActor("https://remote.example/users/x", "https://remote.example/inbox");
        remote.applyFetchedProfile("https://remote.example/inbox",
                                   "pem",
                                   "https://remote.example/users/x#main-key",
                                   "Display",
                                   "handle");
        remoteActorRepository.create(remote);
        var loaded = remoteActorRepository.findByActorId("https://remote.example/users/x");
        assertThat(loaded).isPresent();
        assertThat(loaded.get().getDisplayName()).isEqualTo("Display");
        assertThat(loaded.get().getPreferredUsername()).isEqualTo("handle");
    }

    @Test
    @Transactional
    void parseAndUpsertStoresMastodonStyleActorProfile() {
        var keyPair = keyPairService.generateRsaKeyPair();
        var actorUrl = "https://mastodon.social/users/reader";
        var publicKeyPem = keyPairService.toPublicKeyPem(keyPair.getPublic());
        var body = """
                   {
                     "id": "%s",
                     "type": "Person",
                     "preferredUsername": "reader",
                     "name": "Remote Reader",
                     "inbox": "https://mastodon.social/inbox",
                     "publicKey": {
                       "id": "%s#main-key",
                       "owner": "%s",
                       "publicKeyPem": "%s"
                     }
                   }
                   """.formatted(actorUrl, actorUrl, actorUrl, publicKeyPem.replace("\n", "\\n"));

        var saved = remoteActorService.parseAndUpsert(actorUrl, body);

        assertThat(saved).isPresent();
        assertThat(saved.get().getInboxUrl()).isEqualTo("https://mastodon.social/inbox");
        assertThat(saved.get().getPublicKeyId()).isEqualTo(actorUrl + "#main-key");
        assertThat(saved.get().getDisplayName()).isEqualTo("Remote Reader");
        assertThat(saved.get().getPreferredUsername()).isEqualTo("reader");
        assertThat(remoteActorRepository.findByActorId(actorUrl)).isPresent();
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
    }
}
