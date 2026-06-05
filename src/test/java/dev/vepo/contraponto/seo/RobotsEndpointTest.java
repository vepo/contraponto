package dev.vepo.contraponto.seo;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import io.quarkus.test.common.http.TestHTTPResource;

@QuarkusIntegrationTest
class RobotsEndpointTest {

    @TestHTTPResource
    URL baseUrl;

    @Test
    void robotsDisallowsComponentsAndPrivatePaths() {
        String body = given().when()
                             .get("/robots.txt")
                             .then()
                             .statusCode(200)
                             .extract()
                             .asString();
        org.assertj.core.api.Assertions.assertThat(body).contains("Disallow: /components/");
        org.assertj.core.api.Assertions.assertThat(body).contains("Disallow: /manage");
        org.assertj.core.api.Assertions.assertThat(body).contains("Disallow: /library");
        org.assertj.core.api.Assertions.assertThat(body).contains("Disallow: /feed");
        org.assertj.core.api.Assertions.assertThat(body).contains("Sitemap:");
    }

    @BeforeEach
    void setup() {
        io.restassured.RestAssured.baseURI = baseUrl.getProtocol() + "://" + baseUrl.getHost();
        io.restassured.RestAssured.port = baseUrl.getPort();
        io.restassured.RestAssured.basePath = "";
    }
}
