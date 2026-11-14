package dev.vepo.contraponto.activitypub.inbox;

import static io.restassured.RestAssured.given;

import java.net.URL;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.activitypub.ActivityPubPaths;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusIntegrationTest
@TestProfile(ActivityPubInboxDisabledTest.ActivityPubDisabledTestProfile.class)
class ActivityPubInboxDisabledTest {

    public static final class ActivityPubDisabledTestProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("contraponto.activitypub.enabled", "false");
        }
    }

    @TestHTTPResource("/")
    URL baseUrl;

    @Test
    void federationDisabledReturnsNotFoundBeforeFetch() {
        var body = """
                   {
                     "@context": "https://www.w3.org/ns/activitystreams",
                     "id": "https://remote.example/follow/disabled",
                     "type": "Follow",
                     "actor": "https://remote.example/users/reader",
                     "object": "https://example.com/users/disinboxuser"
                   }
                   """;

        given().contentType(ActivityPubPaths.ACTIVITY_JSON)
               .body(body)
               .post("/disinboxuser/inbox")
               .then()
               .statusCode(404);
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
    }
}
