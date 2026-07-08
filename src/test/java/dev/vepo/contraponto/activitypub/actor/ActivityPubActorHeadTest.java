package dev.vepo.contraponto.activitypub.actor;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.activitypub.ActivityPubPaths;
import dev.vepo.contraponto.blog.BlogSubdomainTestProfile;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.TestProfile;

@QuarkusIntegrationTest
@TestProfile(BlogSubdomainTestProfile.class)
class ActivityPubActorHeadTest {

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
    void getWithActivityJsonAcceptReturnsPersonOnAuthorSubdomain() {
        given().header("Host", "headactor.localhost")
               .accept(ActivityPubPaths.ACTIVITY_JSON)
               .get("/")
               .then()
               .statusCode(200)
               .contentType(ActivityPubPaths.ACTIVITY_JSON)
               .body(containsString("\"type\":\"Person\""))
               .body(containsString("\"preferredUsername\":\"headactor\""));
    }

    @Test
    void headWithActivityJsonAcceptReturns200OnAuthorSubdomain() {
        given().header("Host", "headactor.localhost")
               .accept(ActivityPubPaths.ACTIVITY_JSON)
               .head("/")
               .then()
               .statusCode(200)
               .header("Content-Type", containsString(ActivityPubPaths.ACTIVITY_JSON))
               .header("Content-Length", notNullValue())
               .body(anyOf(emptyOrNullString(), equalTo("")));
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        RestAssuredPort.configure(baseUrl);
        user = Given.user()
                    .withUsername("headactor")
                    .withEmail("headactor@example.com")
                    .withPassword("pw123456789")
                    .withName("Head Actor")
                    .persist();
        Given.activityPubActor().withUser(user).persist();
    }
}
