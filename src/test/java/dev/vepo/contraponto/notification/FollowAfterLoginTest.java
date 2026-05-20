package dev.vepo.contraponto.notification;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.components.forms.LoginEndpoint;
import dev.vepo.contraponto.shared.htmx.HtmxTriggers;
import dev.vepo.contraponto.components.forms.SignUpEndpoint;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebTest;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResource;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class FollowAfterLoginRestTest {

    private static final String LOGGED_IN_BODY_TRIGGER = HtmxTriggers.LOGGED_IN_ON_BODY;

    @TestHTTPResource("/")
    URL baseUrl;

    @Inject
    BlogRepository blogRepository;

    private User author;
    private User reader;
    private long blogId;

    @Test
    void audience_controls_refresh_after_login() {
        given().get("/components/blogs/" + blogId + "/audience")
               .then()
               .statusCode(200)
               .body(containsString("hx-get=\"/auth/modal?mode=login\""));

        var sessionId = Given.inject(dev.vepo.contraponto.shared.infra.LoggedUserProvider.class)
                             .login(reader)
                             .getSessionId();

        given().cookie(LoginEndpoint.SESSION_COOKIE_NAME, sessionId)
               .get("/components/blogs/" + blogId + "/audience")
               .then()
               .statusCode(200)
               .body(containsString("hx-post=\"/forms/blogs/" + blogId + "/follow\""))
               .body(not(containsString("hx-get=\"/auth/modal?mode=login\"")));
    }

    @Test
    void login_form_then_follow_with_session_cookie() {
        var sessionId = given().contentType("application/x-www-form-urlencoded")
                               .formParam("login", "followreader@test.com")
                               .formParam("password", "password123")
                               .post("/forms/auth/login")
                               .then()
                               .statusCode(200)
                               .extract()
                               .cookie(LoginEndpoint.SESSION_COOKIE_NAME);

        var bootstrap = given().cookie(LoginEndpoint.SESSION_COOKIE_NAME, sessionId).get("/");
        String csrf = bootstrap.getCookie(dev.vepo.contraponto.shared.security.CsrfTokenService.COOKIE_NAME);

        given().cookie(LoginEndpoint.SESSION_COOKIE_NAME, sessionId)
               .cookie(dev.vepo.contraponto.shared.security.CsrfTokenService.COOKIE_NAME, csrf)
               .header(dev.vepo.contraponto.shared.security.CsrfTokenService.HEADER_NAME, csrf)
               .post("/forms/blogs/" + blogId + "/follow")
               .then()
               .statusCode(200)
               .body(containsString("Following"));
    }

    @Test
    void login_response_triggers_loggedIn_on_body() {
        given().contentType("application/x-www-form-urlencoded")
               .formParam("login", "followreader@test.com")
               .formParam("password", "password123")
               .post("/forms/auth/login")
               .then()
               .statusCode(200)
               .header("HX-Trigger-After-Settle", LOGGED_IN_BODY_TRIGGER);
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        io.restassured.RestAssured.baseURI = baseUrl.getProtocol() + "://" + baseUrl.getHost();
        io.restassured.RestAssured.port = baseUrl.getPort();
        io.restassured.RestAssured.basePath = "";

        author = Given.user()
                      .withUsername("followauth")
                      .withEmail("followauth@test.com")
                      .withName("Follow Auth")
                      .withPassword("password123")
                      .persist();
        reader = Given.user()
                      .withUsername("followreader")
                      .withEmail("followreader@test.com")
                      .withName("Follow Reader")
                      .withPassword("password123")
                      .persist();
        blogId = blogRepository.findMainByOwnerId(author.getId()).orElseThrow().getId();

        Given.post()
             .withAuthor(author)
             .withTitle("Follow login test post")
             .withSlug("follow-login-test")
             .withContent("Body")
             .withDescription("Desc")
             .withPublished(true)
             .persist();
    }
}

@WebTest
class FollowAfterLoginWebTest {

    private User author;
    private Post post;

    @Test
    void follow_on_post_works_after_login_modal(App app) {
        app.access();
        var postPage = app.goTo(post);
        postPage.assertFollowButtonOpensLoginModal();

        app.loginModal()
           .useLogin("followwebreader@test.com")
           .usePassword("password123")
           .submit()
           .assertModalWasClosed()
           .assertMenuIsDisplayed()
           .assertCookieIsPresent(SignUpEndpoint.SESSION_COOKIE_NAME);

        postPage.waitForReady()
                .assertFollowButtonIsAuthenticated()
                .clickFollowButton();
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        author = Given.user()
                      .withUsername("followweb")
                      .withEmail("followweb@test.com")
                      .withName("Follow Web Author")
                      .withPassword("password123")
                      .persist();
        Given.user()
             .withUsername("followwebreader")
             .withEmail("followwebreader@test.com")
             .withName("Follow Web Reader")
             .withPassword("password123")
             .persist();
        post = Given.post()
                    .withAuthor(author)
                    .withTitle("Follow web test")
                    .withSlug("follow-web-test")
                    .withContent("Post body")
                    .withDescription("Description")
                    .withPublished(true)
                    .persist();
    }
}
