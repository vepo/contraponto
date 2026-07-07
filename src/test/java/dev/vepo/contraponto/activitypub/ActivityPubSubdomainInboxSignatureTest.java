package dev.vepo.contraponto.activitypub;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.security.KeyPair;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.BlogSubdomainContext;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class ActivityPubSubdomainInboxSignatureTest {

    @Inject
    ActivityPubHttpSignatureService signatureService;

    @Inject
    ActivityPubKeyPairService keyPairService;

    @Inject
    BlogSubdomainContext subdomainContext;

    private KeyPair keyPair;

    @Test
    void rejectsInboxSignatureWhenExternalSubdomainPathMissing() {
        var body = "{\"type\":\"Follow\"}";
        var externalInbox = URI.create("https://vepo.commit-mestre.dev/inbox");
        var signed = signatureService.signRequest(keyPair.getPrivate(),
                                                  "https://remote.example/users/reader#main-key",
                                                  "POST",
                                                  externalInbox,
                                                  body);
        var internalInbox = URI.create("https://vepo.commit-mestre.dev/vepo/inbox");

        assertThat(signatureService.verifyRequest("POST", internalInbox, body, signed, keyPair.getPublic())).isFalse();
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        keyPair = keyPairService.generateRsaKeyPair();
    }

    @Test
    void verifiesInboxSignatureUsingExternalSubdomainPathAfterRewrite() {
        var body = "{\"type\":\"Follow\"}";
        var externalInbox = URI.create("https://vepo.commit-mestre.dev/inbox");
        var signed = signatureService.signRequest(keyPair.getPrivate(),
                                                  "https://remote.example/users/reader#main-key",
                                                  "POST",
                                                  externalInbox,
                                                  body);

        subdomainContext.activate("vepo");
        subdomainContext.setSignatureRequestPath("/inbox");
        var internalInbox = URI.create("https://vepo.commit-mestre.dev/vepo/inbox");

        assertThat(signatureService.verifyRequest("POST", internalInbox, body, signed, keyPair.getPublic())).isTrue();
    }
}
