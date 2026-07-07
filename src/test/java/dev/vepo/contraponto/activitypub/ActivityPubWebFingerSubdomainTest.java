package dev.vepo.contraponto.activitypub;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.BlogSubdomainTestProfile;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.TestProfile;

@QuarkusIntegrationTest
@TestProfile(BlogSubdomainTestProfile.class)
class ActivityPubWebFingerSubdomainTest {

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

    @BeforeEach
    void setUp() {
        Given.cleanup();
        RestAssuredPort.configure(baseUrl);
        user = Given.user()
                    .withUsername("subwf")
                    .withEmail("subwf@example.com")
                    .withPassword("pw123456789")
                    .withName("Sub WF")
                    .persist();
        Given.activityPubActor().withUser(user).persist();
    }

    @Test
    void webfingerResolvesActorHostAcctAlias() {
        var actorHostAcct = "acct:subwf@subwf.localhost";
        var json = given().queryParam("resource", actorHostAcct)
                          .accept("application/jrd+json")
                          .get("/.well-known/webfinger")
                          .then()
                          .statusCode(200)
                          .contentType("application/jrd+json")
                          .extract()
                          .body()
                          .asString();
        assertThat(json).contains("\"subject\":\"" + actorHostAcct + "\"")
                        .contains("https://subwf.localhost/");
    }
}
