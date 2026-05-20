package dev.vepo.contraponto.components;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.components.forms.LoginEndpoint;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.infra.LoggedUserProvider;
import io.quarkus.test.common.http.TestHTTPResource;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;

@QuarkusIntegrationTest
class HeaderComponentsTest {

    @TestHTTPResource("/")
    URL baseUrl;

    @Test
    void profilePageRendersAuthenticatedHeaderFragments() {
        var user = Given.user()
                        .withUsername("headerprofile")
                        .withEmail("headerprofile@example.com")
                        .withPassword("pass123")
                        .withName("Header Profile")
                        .persist();
        var sessionId = Given.inject(LoggedUserProvider.class).login(user).getSessionId();

        given().cookie(LoginEndpoint.SESSION_COOKIE_NAME, sessionId)
               .get("/account/security")
               .then()
               .statusCode(200)
               .body(containsString("Escrever"))
               .body(containsString("user-menu"));
    }

    @BeforeEach
    void setup() {
        Given.cleanup();
        io.restassured.RestAssured.baseURI = baseUrl.getProtocol() + "://" + baseUrl.getHost();
        io.restassured.RestAssured.port = baseUrl.getPort();
        io.restassured.RestAssured.basePath = "";
    }

    @Test
    void writeButtonFragmentUsesLoggedUser() {
        var user = Given.user()
                        .withUsername("headerwrite")
                        .withEmail("headerwrite@example.com")
                        .withPassword("pass123")
                        .withName("Header Write")
                        .persist();
        var sessionId = Given.inject(LoggedUserProvider.class).login(user).getSessionId();

        given().cookie(LoginEndpoint.SESSION_COOKIE_NAME, sessionId)
               .get("/components/write-btn")
               .then()
               .statusCode(200)
               .body(containsString("Escrever"));
    }
}
