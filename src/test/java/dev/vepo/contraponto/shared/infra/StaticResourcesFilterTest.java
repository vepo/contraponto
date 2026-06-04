package dev.vepo.contraponto.shared.infra;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.QuarkusIntegrationTest;

@QuarkusIntegrationTest
class StaticResourcesFilterTest {

    @Test
    void servesMainJavaScriptBundle() {
        given().get("/js/main.js")
               .then()
               .statusCode(200)
               .header("Content-Type", containsString("javascript"));
    }

    @Test
    void servesMainStylesheet() {
        given().get("/style/main.css")
               .then()
               .statusCode(200)
               .header("Content-Type", containsString("css"));
    }

    @Test
    void servesSvgWithImageSvgXmlContentType() {
        given().get("/images/search.svg")
               .then()
               .statusCode(200)
               .header("Content-Type", containsString("image/svg+xml"));
    }

    @Test
    void servesWriteSvgWithImageSvgXmlContentType() {
        given().get("/images/write.svg")
               .then()
               .statusCode(200)
               .header("Content-Type", containsString("image/svg+xml"));
    }
}
