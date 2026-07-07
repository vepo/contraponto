package dev.vepo.contraponto.activitypub;

import static io.restassured.RestAssured.given;

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
class ActivityPubInboxEndpointTest {

    private static final class RestAssuredPort {
        static void configure(URL baseUrl) {
            io.restassured.RestAssured.baseURI = baseUrl.getProtocol() + "://" + baseUrl.getHost();
            io.restassured.RestAssured.port = baseUrl.getPort();
            io.restassured.RestAssured.basePath = "";
        }

        private RestAssuredPort() {}
    }

    @TestHTTPResource("/")
    URL baseUrl;

    @Inject
    BlogSubdomainConfig subdomainConfig;

    private User user;

    @Test
    void rejectsUnsignedFollow() {
        var actorId = ActivityPubPaths.actorId(user, subdomainConfig);
        var body = """
                   {
                     "@context": "https://www.w3.org/ns/activitystreams",
                     "id": "https://remote.example/follow/1",
                     "type": "Follow",
                     "actor": "https://remote.example/users/reader",
                     "object": "%s"
                   }
                   """.formatted(actorId);
        given().contentType(ActivityPubPaths.ACTIVITY_JSON)
               .body(body)
               .post("/inboxuser/inbox")
               .then()
               .statusCode(401);
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        RestAssuredPort.configure(baseUrl);
        user = Given.user()
                    .withUsername("inboxuser")
                    .withEmail("inboxuser@example.com")
                    .withPassword("pw123456789")
                    .withName("Inbox User")
                    .persist();
        Given.activityPubActor().withUser(user).persist();
    }
}
