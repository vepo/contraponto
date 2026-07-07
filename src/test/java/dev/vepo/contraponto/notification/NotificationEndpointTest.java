package dev.vepo.contraponto.notification;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.messaging.MessageComposeService;
import dev.vepo.contraponto.messaging.MessageThreadPaths;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.TestHttp;
import dev.vepo.contraponto.shared.htmx.HtmxTriggers;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResource;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@QuarkusIntegrationTest
class NotificationEndpointTest {

    @TestHTTPResource("/")
    URL baseUrl;

    @Inject
    NotificationRepository notificationRepository;

    @Inject
    NotificationService notificationService;

    @Inject
    MessageComposeService messageComposeService;

    @Inject
    BlogRepository blogRepository;

    @Inject
    EntityManager entityManager;

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
        entityManager.clear();
        var dismissed = entityManager.find(Notification.class, notificationId);
        assertThat(dismissed.isRead()).isTrue();
        assertThat(dismissed.getReadAt()).isNotNull();
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
        entityManager.clear();
        notificationRepository.findPage(recipient.getId(), dev.vepo.contraponto.shared.pagination.PageQuery.forGrid(20, 1))
                              .data()
                              .forEach(notification -> assertThat(notification.getReadAt()).isNotNull());
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
                          .body(containsString("started following"))
                          .body(containsString("/style/manage.css"));
    }

    @Test
    void notificationsPage_messageNotificationLinksToThread() {
        var sender = Given.user()
                          .withUsername("inboxpgsender")
                          .withEmail("inboxpgsender@test.com")
                          .withName("Inbox Page Sender")
                          .withPassword("password123")
                          .persist();
        var thread = messageComposeService.compose(sender.getId(),
                                                   recipient.getUsername(),
                                                   "Follow-up",
                                                   "Checking the inbox page link.");
        String threadPath = MessageThreadPaths.thread(thread.getId());

        session(recipient).get("/account/notifications")
                          .then()
                          .statusCode(200)
                          .body(containsString("started a message thread"))
                          .body(containsString("href=\"%s\"".formatted(threadPath)))
                          .body(containsString("data-hx-get=\"%s\"".formatted(threadPath)));
    }

    @Test
    void notificationsPage_messageNotificationRowDoesNotUseBlogUrl() {
        var sender = Given.user()
                          .withUsername("inboxmsgsndr")
                          .withEmail("inboxmsgsndr@test.com")
                          .withName("Inbox Msg Sender")
                          .withPassword("password123")
                          .persist();
        var blog = blogRepository.findMainByOwnerId(sender.getId()).orElseThrow();
        var thread = messageComposeService.compose(sender.getId(),
                                                   recipient.getUsername(),
                                                   "Thread link check",
                                                   "Message body.");
        String threadPath = MessageThreadPaths.thread(thread.getId());
        String blogPath = "/%s".formatted(sender.getUsername());

        notificationService.notifyNewFollow(recipient, blog, sender);

        String html = session(recipient).get("/account/notifications")
                                        .then()
                                        .statusCode(200)
                                        .extract()
                                        .body()
                                        .asString();

        int messageIdx = html.indexOf("started a message thread");
        assertThat(messageIdx).isPositive();
        String messageRow = html.substring(Math.max(0, messageIdx - 400), Math.min(html.length(), messageIdx + 200));
        assertThat(messageRow).contains("href=\"%s\"".formatted(threadPath));
        assertThat(messageRow).doesNotContain("href=\"%s\"".formatted(blogPath));

        int followIdx = html.indexOf("started following");
        assertThat(followIdx).isPositive();
        String followRow = html.substring(Math.max(0, followIdx - 400), Math.min(html.length(), followIdx + 200));
        assertThat(followRow).contains("href=\"%s\"".formatted(blogPath));
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

    @Test
    void overlay_messageNotificationLinksToThread() {
        var sender = Given.user()
                          .withUsername("inbox-sender")
                          .withEmail("inbox-sender@test.com")
                          .withName("Inbox Sender")
                          .withPassword("password123")
                          .persist();
        var thread = messageComposeService.compose(sender.getId(),
                                                   recipient.getUsername(),
                                                   "Question",
                                                   "Can we pair on messaging?");
        String threadPath = MessageThreadPaths.thread(thread.getId());

        session(recipient).get("/components/notifications/overlay")
                          .then()
                          .statusCode(200)
                          .body(containsString("started a message thread"))
                          .body(containsString("href=\"%s\"".formatted(threadPath)))
                          .body(containsString("data-hx-get=\"%s\"".formatted(threadPath)));
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
