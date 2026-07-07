package dev.vepo.contraponto.activitypub;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResource;

@QuarkusIntegrationTest
class ActivityPubOutboxTest {

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

    private User user;

    @Test
    void actorJsonReturnsPersonWithInboxAndOutbox() {
        var json = given().accept(ActivityPubPaths.ACTIVITY_JSON)
                          .get("/outboxuser")
                          .then()
                          .statusCode(200)
                          .extract()
                          .body()
                          .asString();
        assertThat(json).contains("\"type\":\"Person\"")
                        .contains("\"inbox\"")
                        .contains("\"outbox\"")
                        .contains("outboxuser");
    }

    @Test
    void disabledActorReturns404() {
        Given.activityPubActor().withUser(user).withFederationEnabled(false).persist();
        given().accept(ActivityPubPaths.ACTIVITY_JSON)
               .get("/outboxuser/outbox")
               .then()
               .statusCode(404);
        given().accept(ActivityPubPaths.ACTIVITY_JSON)
               .get("/outboxuser")
               .then()
               .statusCode(404);
    }

    @Test
    void outboxListsCreateAfterPublish() {
        var json = given().accept(ActivityPubPaths.ACTIVITY_JSON)
                          .get("/outboxuser/outbox")
                          .then()
                          .statusCode(200)
                          .extract()
                          .body()
                          .asString();
        assertThat(json).contains("\"type\":\"OrderedCollection\"")
                        .contains("\"type\":\"Create\"")
                        .contains("Outbox Post");
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        RestAssuredPort.configure(baseUrl);
        user = Given.user()
                    .withUsername("outboxuser")
                    .withEmail("outboxuser@example.com")
                    .withPassword("pw123456789")
                    .withName("Outbox User")
                    .persist();
        Given.activityPubActor().withUser(user).persist();
        Given.post()
             .withAuthor(user)
             .withTitle("Outbox Post")
             .withSlug("outbox-post")
             .withDescription("Summary")
             .withContent("Body")
             .persist();
    }
}
