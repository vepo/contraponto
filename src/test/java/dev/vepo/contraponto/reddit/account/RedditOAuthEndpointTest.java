package dev.vepo.contraponto.reddit.account;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;

import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.shared.TestHttp;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResource;
import jakarta.inject.Inject;

/**
 * Covers the guard clauses reachable without ever calling Reddit — the happy
 * path (valid code + state) necessarily calls the real Reddit token endpoint,
 * which this suite deliberately never does.
 */
@QuarkusIntegrationTest
class RedditOAuthEndpointTest {

    @TestHTTPResource("/")
    URL baseUrl;

    @Inject
    UserRedditAccountRepository accountRepository;

    private User user;

    @Test
    void callbackWithErrorParamRedirectsWithoutLinking() {
        TestHttp.authenticated(user)
                .redirects()
                .follow(false)
                .get("/writing/reddit/callback?error=access_denied")
                .then()
                .statusCode(303)
                .header("Location", containsString("/writing/appearance"));

        assertThat(accountRepository.findByUserId(user.getId())).isEmpty();
    }

    @Test
    void callbackWithInvalidStateRedirectsWithoutLinking() {
        TestHttp.authenticated(user)
                .redirects()
                .follow(false)
                .get("/writing/reddit/callback?code=somecode&state=forged-state")
                .then()
                .statusCode(303)
                .header("Location", containsString("/writing/appearance"));

        assertThat(accountRepository.findByUserId(user.getId())).isEmpty();
    }

    @Test
    void callbackWithoutSessionRedirectsToSignIn() {
        // @Logged intercepts before the endpoint's own auth check runs — same
        // platform sign-in redirect as any other protected full-page GET.
        given().redirects()
               .follow(false)
               .get("/writing/reddit/callback?code=somecode&state=whatever")
               .then()
               .statusCode(303)
               .header("Location", containsString("signIn=1"));
    }

    @Test
    void connectRedirectsToRedditAuthorizeUrlWithExpectedParams() {
        TestHttp.authenticated(user)
                .redirects()
                .follow(false)
                .get("/writing/reddit/connect")
                .then()
                .statusCode(303)
                .header("Location", startsWith("https://www.reddit.com/api/v1/authorize?"))
                .header("Location", containsString("client_id=test-client-id"))
                .header("Location", containsString("duration=permanent"))
                .header("Location", containsString("state="));
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        io.restassured.RestAssured.baseURI = baseUrl.getProtocol() + "://" + baseUrl.getHost();
        io.restassured.RestAssured.port = baseUrl.getPort();
        io.restassured.RestAssured.basePath = "";

        user = Given.user()
                    .withUsername("redditoauth")
                    .withEmail("redditoauth@example.com")
                    .withPassword("redditOauthPw1")
                    .withName("Reddit OAuth User")
                    .persist();
    }
}
