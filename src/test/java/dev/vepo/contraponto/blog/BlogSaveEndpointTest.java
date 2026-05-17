package dev.vepo.contraponto.blog;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.components.forms.LoginEndpoint;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.infra.LoggedUserProvider;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class BlogSaveEndpointTest {

    @TestHTTPResource("/")
    URL baseUrl;

    private User owner;

    @Test
    void create_blog_returns_success_toast_headers() {
        var sessionId = Given.inject(LoggedUserProvider.class).login(owner).getSessionId();

        given().cookie(LoginEndpoint.SESSION_COOKIE_NAME, sessionId)
               .contentType("application/x-www-form-urlencoded")
               .formParam("name", "Travel Blog")
               .formParam("slug", "travel")
               .formParam("description", "Trips")
               .formParam("active", "on")
               .post("/forms/blogs")
               .then()
               .statusCode(200)
               .header("X-Toast-Message", equalTo("Blog saved successfully."))
               .header("X-Toast-Type", equalTo("Success"))
               .header("HX-Trigger", equalTo("{\"toast:show\":{\"message\":\"Blog saved successfully.\",\"duration\":10000,\"type\":\"Success\"}}"))
               .header("HX-Trigger-After-Settle",
                       equalTo("{\"toast:show\":{\"message\":\"Blog saved successfully.\",\"duration\":10000,\"type\":\"Success\"}}"));
    }

    @BeforeEach
    void setup() {
        Given.cleanup();
        io.restassured.RestAssured.baseURI = baseUrl.getProtocol() + "://" + baseUrl.getHost();
        io.restassured.RestAssured.port = baseUrl.getPort();
        io.restassured.RestAssured.basePath = "";

        owner = Given.user()
                     .withUsername("blogsave")
                     .withEmail("blogsave@example.com")
                     .withPassword("blogSavePw1")
                     .withName("Blog Save Owner")
                     .persist();
    }
}
