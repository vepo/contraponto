package dev.vepo.contraponto.shared.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
class I18nMessagesEndpointTest {

    @Test
    void shouldNotServeDefaultLocaleBundle() {
        RestAssured.given()
                   .when()
                   .get("/i18n/messages/pt-BR.json")
                   .then()
                   .statusCode(404);
    }

    @Test
    void shouldServeEnglishBundle() {
        var body = RestAssured.given()
                              .when()
                              .get("/i18n/messages/en.json")
                              .then()
                              .statusCode(200)
                              .extract()
                              .body()
                              .asString();
        assertThat(body).contains("\"auth.signIn\"");
        assertThat(body).contains("Sign in");
    }
}
