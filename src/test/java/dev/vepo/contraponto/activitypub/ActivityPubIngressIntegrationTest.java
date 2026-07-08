package dev.vepo.contraponto.activitypub;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

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
class ActivityPubIngressIntegrationTest {

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
    void apiNodeinfoOnSubdomainReturnsNodeInfoDocument() {
        given().header("Host", "ingress.localhost")
               .get("/api/nodeinfo")
               .then()
               .statusCode(200)
               .contentType("application/json")
               .body("version", equalTo("2.0"))
               .body("protocols[0]", equalTo("activitypub"));
    }

    @Test
    void nodeinfo20ReturnsJsonDocument() {
        given().get("/nodeinfo/2.0")
               .then()
               .statusCode(200)
               .contentType("application/json")
               .body("software.name", equalTo("contraponto"));
    }

    @Test
    void pocoProbeReturns404WithoutBlogLookup() {
        given().header("Host", "ingress.localhost")
               .get("/ingress/poco")
               .then()
               .statusCode(404);
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        RestAssuredPort.configure(baseUrl);
        user = Given.user()
                    .withUsername("ingress")
                    .withEmail("ingress@example.com")
                    .withPassword("pw123456789")
                    .withName("Ingress User")
                    .persist();
        Given.activityPubActor().withUser(user).persist();
    }

    @Test
    void wellKnownNodeinfoReturnsJrd() {
        given().accept("application/jrd+json")
               .get("/.well-known/nodeinfo")
               .then()
               .statusCode(200)
               .contentType("application/jrd+json")
               .body("links[0].rel", equalTo("http://nodeinfo.diaspora.software/ns/schema/1.0"));
    }
}
