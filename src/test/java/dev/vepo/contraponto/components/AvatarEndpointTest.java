package dev.vepo.contraponto.components;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class AvatarEndpointTest {

    @TestHTTPResource("/")
    URL baseUrl;

    @Test
    void avatarRejectsBlankName() {
        given().queryParam("name", " ")
               .get("/components/avatar")
               .then()
               .statusCode(400);
    }

    @Test
    void avatarRejectsMissingName() {
        given().get("/components/avatar")
               .then()
               .statusCode(400);
    }

    @Test
    void avatarReturnsSvgWithInitials() {
        given().queryParam("name", "Ada Lovelace")
               .get("/components/avatar")
               .then()
               .statusCode(200)
               .contentType("image/svg+xml")
               .header("Cache-Control", containsString("max-age=86400"))
               .body(containsString(">AL</text>"));
    }

    @BeforeEach
    void setup() {
        io.restassured.RestAssured.baseURI = baseUrl.getProtocol() + "://" + baseUrl.getHost();
        io.restassured.RestAssured.port = baseUrl.getPort();
        io.restassured.RestAssured.basePath = "";
    }
}
