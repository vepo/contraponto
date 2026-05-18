package dev.vepo.contraponto.shared;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.components.forms.LoginEndpoint;
import dev.vepo.contraponto.shared.infra.LoggedUserProvider;
import dev.vepo.contraponto.shared.security.CsrfTokenService;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class CsrfBootstrapTest {

    @Test
    void homePageIncludesCsrfMetaWhenAuthenticated() {
        var user = Given.user()
                        .withUsername("csrfmeta")
                        .withEmail("csrfmeta@example.com")
                        .withPassword("password1234")
                        .withName("Csrf Meta")
                        .persist();
        var sessionId = Given.inject(LoggedUserProvider.class).login(user).getSessionId();

        var response = given().redirects()
                              .follow(true)
                              .cookie(LoginEndpoint.SESSION_COOKIE_NAME, sessionId)
                              .when()
                              .get("/");

        assertThat(response.getCookie(CsrfTokenService.COOKIE_NAME)).isNotBlank();
        assertThat(response.getBody().asString()).contains("name=\"csrf-token\"");
        assertThat(response.getBody().asString()).contains("content=\"" + response.getCookie(CsrfTokenService.COOKIE_NAME) + "\"");
    }
}
