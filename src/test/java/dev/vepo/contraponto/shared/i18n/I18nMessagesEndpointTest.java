package dev.vepo.contraponto.shared.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import io.restassured.RestAssured;

@QuarkusIntegrationTest
class I18nMessagesEndpointTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Set<String> ALLOW_SAME_AS_EN = Set.of(
                                                               "locale.en",
                                                               "locale.es",
                                                               "locale.pt",
                                                               "rss.link",
                                                               "locale.name.en",
                                                               "locale.name.es",
                                                               "locale.name.ptBr",
                                                               "dashboard.blogSelector",
                                                               "hub.blogs");

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

    @Test
    void shouldServeSpanishBundleWithSameKeysAsEnglish() throws Exception {
        var enJson = RestAssured.given()
                                .when()
                                .get("/i18n/messages/en.json")
                                .then()
                                .statusCode(200)
                                .extract()
                                .asString();
        var esJson = RestAssured.given()
                                .when()
                                .get("/i18n/messages/es.json")
                                .then()
                                .statusCode(200)
                                .extract()
                                .asString();

        Map<String, String> en = MAPPER.readValue(enJson, new TypeReference<>() {});
        Map<String, String> es = MAPPER.readValue(esJson, new TypeReference<>() {});

        assertThat(es.keySet()).isEqualTo(en.keySet());

        var untranslated = en.entrySet().stream()
                             .filter(e -> es.get(e.getKey()).equals(e.getValue()))
                             .filter(e -> !ALLOW_SAME_AS_EN.contains(e.getKey()))
                             .map(Map.Entry::getKey)
                             .toList();
        assertThat(untranslated)
                                .as("Spanish bundle should not copy English for user-facing keys")
                                .isEmpty();
    }
}
