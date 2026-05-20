package dev.vepo.contraponto.notification;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.TestHttp;
import dev.vepo.contraponto.shared.htmx.HtmxTriggers;
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
    void badge_shows_menu_chrome_and_unread_count() {
        session(recipient).get("/components/notifications/badge")
                          .then()
                          .statusCode(200)
                          .body(containsString("notificationBellBtn"))
                          .body(containsString("notification-bell__badge"))
                          .body(containsString("notificationOverlay"));
    }

    @Test
    void dismiss_single_notification_updates_overlay_and_count() {
        var notificationId = notificationRepository.findUnreadRecent(recipient.getId(), 1).getFirst().getId();
        assertThat(notificationRepository.countUnread(recipient.getId())).isPositive();

        session(recipient).header("HX-Request", "true")
                          .header("HX-Target", "notificationOverlay")
                          .post("/forms/notifications/" + notificationId + "/dismiss")
                          .then()
                          .statusCode(200)
                          .header(HtmxTriggers.HEADER_AFTER_SETTLE, containsString("notificationsChanged"));

        assertThat(notificationRepository.countUnread(recipient.getId())).isZero();
    }

    @Test
    void mark_all_read_from_inbox_returns_hub_panel() {
        assertThat(notificationRepository.countUnread(recipient.getId())).isPositive();

        session(recipient).header("HX-Request", "true")
                          .header("HX-Target", "hub-panel")
                          .post("/forms/notifications/read")
                          .then()
                          .statusCode(200)
                          .header(HtmxTriggers.HEADER_AFTER_SETTLE, containsString("notificationsChanged"))
                          .body(not(containsString("notification-list__item--unread")));

        assertThat(notificationRepository.countUnread(recipient.getId())).isZero();
    }

    @Test
    void mark_all_read_from_overlay_returns_html_and_trigger() {
        assertThat(notificationRepository.countUnread(recipient.getId())).isPositive();

        session(recipient).header("HX-Request", "true")
                          .header("HX-Target", "notificationOverlay")
                          .post("/forms/notifications/read")
                          .then()
                          .statusCode(200)
                          .header(HtmxTriggers.HEADER_AFTER_SETTLE, containsString("notificationsChanged"))
                          .body(containsString("Nenhuma notificação"));

        assertThat(notificationRepository.countUnread(recipient.getId())).isZero();
    }

    @Test
    void notifications_page_lists_items() {
        session(recipient).get("/account/notifications")
                          .then()
                          .statusCode(200)
                          .body(containsString("started following"));
    }

    @Test
    void overlay_lists_unread_with_dismiss_controls() {
        session(recipient).get("/components/notifications/overlay")
                          .then()
                          .statusCode(200)
                          .body(containsString("started following"))
                          .body(containsString("Dispensar"))
                          .body(containsString("Marcar tudo como lido"));
    }

    private io.restassured.specification.RequestSpecification session(User user) {
        return TestHttp.authenticated(user);
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
