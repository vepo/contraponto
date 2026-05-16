package dev.vepo.contraponto.notification;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.components.forms.LoginEndpoint;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.infra.LoggedUserProvider;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class NotificationEndpointTest {

    @TestHTTPResource("/")
    URL baseUrl;

    @Inject
    NotificationRepository notificationRepository;

    @Inject
    NotificationService notificationService;

    @Inject
    BlogRepository blogRepository;

    private User recipient;

    @Test
    void badge_shows_unread_count() {
        session(recipient).get("/components/notifications/badge")
                          .then()
                          .statusCode(200)
                          .body(containsString("notification-bell__badge"));
    }

    @Test
    void mark_all_read_clears_unread() {
        assertThat(notificationRepository.countUnread(recipient.getId())).isPositive();

        session(recipient).redirects().follow(false)
                          .post("/forms/notifications/read")
                          .then()
                          .statusCode(303);

        assertThat(notificationRepository.countUnread(recipient.getId())).isZero();
    }

    @Test
    void notifications_page_lists_items() {
        session(recipient).get("/notifications")
                          .then()
                          .statusCode(200)
                          .body(containsString("started following"));
    }

    private io.restassured.specification.RequestSpecification session(User user) {
        var sessionId = Given.inject(LoggedUserProvider.class).login(user).getSessionId();
        return given().cookie(LoginEndpoint.SESSION_COOKIE_NAME, sessionId);
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        io.restassured.RestAssured.baseURI = baseUrl.getProtocol() + "://" + baseUrl.getHost();
        io.restassured.RestAssured.port = baseUrl.getPort();
        io.restassured.RestAssured.basePath = "";

        var author = Given.user()
                          .withUsername("inboxauthor")
                          .withEmail("inboxauthor@test.com")
                          .withName("Inbox Author")
                          .withPassword("password123")
                          .persist();
        recipient = Given.user()
                         .withUsername("inboxuser")
                         .withEmail("inboxuser@test.com")
                         .withName("Inbox User")
                         .withPassword("password123")
                         .persist();
        var blog = blogRepository.findMainByOwnerId(author.getId()).orElseThrow();
        var actor = Given.user()
                         .withUsername("inboxactor")
                         .withEmail("inboxactor@test.com")
                         .withName("Inbox Actor")
                         .withPassword("password123")
                         .persist();
        notificationService.notifyNewFollow(recipient, blog, actor);
    }
}
