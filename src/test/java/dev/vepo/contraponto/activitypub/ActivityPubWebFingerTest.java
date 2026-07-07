package dev.vepo.contraponto.activitypub;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

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
class ActivityPubWebFingerTest {

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

    @BeforeEach
    void setUp() {
        Given.cleanup();
        RestAssuredPort.configure(baseUrl);
        user = Given.user()
                    .withUsername("wfuser")
                    .withEmail("wfuser@example.com")
                    .withPassword("pw123456789")
                    .withName("WF User")
                    .persist();
        Given.activityPubActor().withUser(user).persist();
    }

    @Test
    void webfingerResolvesAcctResource() {
        var resource = ActivityPubPaths.acctHandle(user, subdomainConfig);
        var json = given().queryParam("resource", resource)
                          .accept(ActivityPubPaths.ACTIVITY_JSON)
                          .get("/.well-known/webfinger")
                          .then()
                          .statusCode(200)
                          .extract()
                          .body()
                          .asString();
        assertThat(json).contains("\"subject\":\"" + resource + "\"")
                        .contains("\"rel\":\"self\"")
                        .contains("/wfuser");
    }

    @Test
    void webfingerUnknownUserReturns404() {
        given().queryParam("resource", "acct:nobody@localhost")
               .get("/.well-known/webfinger")
               .then()
               .statusCode(404);
    }
}
