package dev.vepo.contraponto.activitypub.delivery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.LinkedHashMap;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.UnitTest;

@UnitTest
class ActivityPubDeliveryHeadersTest {

    @Test
    void applySignedHeadersSetsHostWhenRestrictedHeadersAllowed() {
        var signed = new LinkedHashMap<String, String>();
        signed.put("Host", "mastodon.social");
        signed.put("Date", "Tue, 08 Jul 2026 00:00:00 GMT");
        signed.put("Digest", "SHA-256=abc");
        signed.put("Signature",
                   "keyId=\"https://example/users/a#main-key\",headers=\"(request-target) host date digest\"");

        var builder = HttpRequest.newBuilder(URI.create("https://mastodon.social/inbox"))
                                 .POST(HttpRequest.BodyPublishers.noBody());

        assertThatCode(() -> ActivityPubDeliveryService.applySignedHeaders(builder, signed)).doesNotThrowAnyException();

        var request = builder.build();
        assertThat(request.headers().firstValue("Host")).contains("mastodon.social");
        assertThat(request.headers().firstValue("Date")).contains("Tue, 08 Jul 2026 00:00:00 GMT");
        assertThat(request.headers().firstValue("Digest")).contains("SHA-256=abc");
        assertThat(request.headers().firstValue("Signature")).isPresent();
    }
}
