package dev.vepo.contraponto.shared.infra;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class StaticResourcesFilterTest {

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
