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
                          .accept("application/jrd+json")
                          .get("/.well-known/webfinger")
                          .then()
                          .statusCode(200)
                          .contentType("application/jrd+json")
                          .extract()
                          .body()
                          .asString();
        assertThat(json).contains("\"subject\":\"" + resource + "\"")
                        .contains("\"rel\":\"self\"")
                        .contains("http://webfinger.net/rel/profile-page")
                        .contains("http://ostatus.org/schema/1.0/subscribe")
                        .contains("?acct={uri}")
                        .contains("/authors/wfuser")
                        .contains("/wfuser");
    }

    @Test
    void webfingerResolvesAcctResourceCaseInsensitively() {
        var domain = ActivityPubPaths.webFingerHandle(user, subdomainConfig).substring(user.getUsername().length() + 1);
        var mixedCaseResource = "acct:WFUSER@%s".formatted(domain);
        given().queryParam("resource", mixedCaseResource)
               .accept("application/jrd+json")
               .get("/.well-known/webfinger")
               .then()
               .statusCode(200)
               .contentType("application/jrd+json");
    }

    @Test
    void webfingerSelfHrefMatchesActorId() {
        var resource = ActivityPubPaths.acctHandle(user, subdomainConfig);
        var selfHref = given().queryParam("resource", resource)
                              .accept("application/jrd+json")
                              .get("/.well-known/webfinger")
                              .then()
                              .statusCode(200)
                              .extract()
                              .path("links.find { it.rel == 'self' }.href");
        var actorId = given().accept(ActivityPubPaths.ACTIVITY_JSON)
                             .get("/%s".formatted(user.getUsername()))
                             .then()
                             .statusCode(200)
                             .extract()
                             .path("id");
        assertThat(actorId).isEqualTo(selfHref);
        var webfinger = given().accept(ActivityPubPaths.ACTIVITY_JSON)
                               .get("/%s".formatted(user.getUsername()))
                               .then()
                               .statusCode(200)
                               .extract()
                               .path("webfinger");
        var loopbackSelfHref = given().queryParam("resource", "acct:%s".formatted(webfinger))
                                      .accept("application/jrd+json")
                                      .get("/.well-known/webfinger")
                                      .then()
                                      .statusCode(200)
                                      .extract()
                                      .path("links.find { it.rel == 'self' }.href");
        assertThat(loopbackSelfHref).isEqualTo(actorId);
    }

    @Test
    void webfingerUnknownUserReturns404() {
        given().queryParam("resource", "acct:nobody@localhost")
               .get("/.well-known/webfinger")
               .then()
               .statusCode(404);
    }
}
