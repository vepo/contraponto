package dev.vepo.contraponto.activitypub;

import static io.restassured.RestAssured.given;

import java.net.URL;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.TestProfile;

@QuarkusIntegrationTest
@TestProfile(ActivityPubInboxDisabledTest.ActivityPubDisabledTestProfile.class)
class ActivityPubInboxDisabledTest {

    public static final class ActivityPubDisabledTestProfile implements io.quarkus.test.junit.QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("contraponto.activitypub.enabled", "false");
        }
    }

    @TestHTTPResource("/")
    URL baseUrl;

    private User user;

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
        io.restassured.RestAssured.baseURI = baseUrl.getProtocol() + "://" + baseUrl.getHost();
        io.restassured.RestAssured.port = baseUrl.getPort();
        io.restassured.RestAssured.basePath = "";
        user = Given.user()
                    .withUsername("disinboxuser")
                    .withEmail("disinbox@example.com")
                    .withPassword("pw123456789")
                    .withName("Disabled Inbox User")
                    .persist();
    }
}
