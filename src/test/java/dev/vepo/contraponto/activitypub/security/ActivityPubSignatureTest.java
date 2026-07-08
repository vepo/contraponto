package dev.vepo.contraponto.activitypub.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.activitypub.actor.ActivityPubActor;
import dev.vepo.contraponto.activitypub.actor.ActivityPubActorRepository;
import dev.vepo.contraponto.activitypub.actor.ActivityPubKeyPairService;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.user.User;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class ActivityPubSignatureTest {

    @Inject
    ActivityPubKeyPairService keyPairService;

    @Inject
    ActivityPubHttpSignatureService signatureService;

    @Inject
    ActivityPubActorRepository actorRepository;

    private ActivityPubActor actor;

    @Test
    void computeDigestIsStable() {
        assertThat(signatureService.computeDigest("hello"))
                                                           .isEqualTo(signatureService.computeDigest("hello"))
                                                           .startsWith("SHA-256=");
    }

    @Test
    void httpDateZeroPadsSingleDigitDay() {
        var date = ActivityPubHttpSignatureService.formatHttpDate(ZonedDateTime.of(2026, 7, 8, 12, 34, 56, 0, ZoneOffset.UTC));
        assertThat(date).isEqualTo("Wed, 08 Jul 2026 12:34:56 GMT");
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        User user = Given.user()
                         .withUsername("siguser")
                         .withEmail("siguser@example.com")
                         .withPassword("pw123456789")
                         .withName("Sig User")
                         .persist();
        actor = Given.activityPubActor().withUser(user).persist();
    }

    @Test
    void signAndVerifyRoundTrip() {
        var keyPair = keyPairService.generateRsaKeyPair();
        var privateKey = keyPair.getPrivate();
        var publicKey = keyPair.getPublic();
        var body = "{\"type\":\"Follow\"}";
        var target = URI.create("https://remote.example/inbox");
        var signed = signatureService.signRequest(privateKey, "https://actor.example/#mainKey", "POST", target, body);
        assertThat(signatureService.verifyRequest("POST", target, body, signed, publicKey)).isTrue();
    }

    @Test
    void signAndVerifyRoundTripWithStoredActorKeys() {
        var privateKey = keyPairService.decryptPrivateKey(actor.getPrivateKeyEncrypted());
        var publicKey = keyPairService.parsePublicKeyPem(actor.getPublicKeyPem());
        var body = "{\"type\":\"Follow\"}";
        var target = URI.create("https://remote.example/inbox");
        var signed = signatureService.signRequest(privateKey, actor.getPublicKeyId(), "POST", target, body);
        assertThat(signatureService.verifyRequest("POST", target, body, signed, publicKey)).isTrue();
        assertThat(signatureService.verifyRequest("POST", target, body, signed)).isTrue();
    }

    @Test
    void signRequestDateUsesZeroPaddedHttpDate() {
        var privateKey = keyPairService.decryptPrivateKey(actor.getPrivateKeyEncrypted());
        var signed = signatureService.signRequest(privateKey,
                                                  actor.getPublicKeyId(),
                                                  "POST",
                                                  URI.create("https://remote.example/inbox"),
                                                  "{\"type\":\"Create\"}");
        assertThat(signed.get("Date")).matches("^[A-Za-z]{3}, \\d{2} [A-Za-z]{3} \\d{4} \\d{2}:\\d{2}:\\d{2} GMT$");
    }

    @Test
    void verifyRejectsTamperedBody() {
        var privateKey = keyPairService.decryptPrivateKey(actor.getPrivateKeyEncrypted());
        var body = "{\"type\":\"Follow\"}";
        var target = URI.create("https://remote.example/inbox");
        var signed = signatureService.signRequest(privateKey, actor.getPublicKeyId(), "POST", target, body);
        assertThat(signatureService.verifyRequest("POST", target, "{\"type\":\"Undo\"}", signed)).isFalse();
    }
}
