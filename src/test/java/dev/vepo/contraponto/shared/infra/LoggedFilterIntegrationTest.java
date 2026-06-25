package dev.vepo.contraponto.shared.infra;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;

import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import io.quarkus.test.common.http.TestHTTPResource;

@QuarkusIntegrationTest
class LoggedFilterIntegrationTest {

    @TestHTTPResource("/")
    URL baseUrl;

    @Test
    void protectedHub_htmxRequestDoesNotRedirectToHome() {
        given().header("HX-Request", "true")
               .redirects()
               .follow(false)
               .when()
               .get("/administration")
               .then()
               .statusCode(401)
               .header("Location", nullValue());
    }

    @Test
    void protectedHub_htmxRequestReturnsUnauthorizedWithoutHomeRedirect() {
        given().header("HX-Request", "true")
               .redirects()
               .follow(false)
               .when()
               .get("/administration")
               .then()
               .statusCode(401)
               .header("HX-Trigger-After-Settle", containsString("loginRequired"))
               .header("HX-Trigger-After-Settle", containsString("/administration"));
    }

    @Test
    void protectedHub_redirectsToSignInWithReturnTo() {
        given().redirects()
               .follow(false)
               .when()
               .get("/administration")
               .then()
               .statusCode(303)
               .header("Location", containsString("signIn=1"))
               .header("Location", containsString("returnTo=%2Fadministration"));
    }

    @BeforeEach
    void setUp() {
        io.restassured.RestAssured.baseURI = "http://127.0.0.1";
        io.restassured.RestAssured.port = baseUrl.getPort();
        io.restassured.RestAssured.basePath = "";
    }
}
