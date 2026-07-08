package dev.vepo.contraponto.activitypub.discovery;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.activitypub.ActivityPubPaths;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResource;

@QuarkusIntegrationTest
class ActivityPubFollowingEndpointTest {

    @TestHTTPResource("/")
    URL baseUrl;

    private User user;

    @Test
    void followingCollectionReturnsActivityJson() {
        given().accept(ActivityPubPaths.ACTIVITY_JSON)
               .get("/%s/following".formatted(user.getUsername()))
               .then()
               .statusCode(200)
               .contentType(ActivityPubPaths.ACTIVITY_JSON)
               .body("type", equalTo("Collection"))
               .body("totalItems", equalTo(0))
               .body("items", hasSize(0));
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        io.restassured.RestAssured.baseURI = baseUrl.getProtocol() + "://" + baseUrl.getHost();
        io.restassured.RestAssured.port = baseUrl.getPort();
        io.restassured.RestAssured.basePath = "";
        user = Given.user()
                    .withUsername("followinguser")
                    .withEmail("following@example.com")
                    .withPassword("pw123456789")
                    .withName("Following User")
                    .persist();
        Given.activityPubActor().withUser(user).persist();
    }
}
