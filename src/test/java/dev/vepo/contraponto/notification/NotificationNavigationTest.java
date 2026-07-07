package dev.vepo.contraponto.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.messaging.MessageComposeService;
import dev.vepo.contraponto.messaging.MessageThreadPaths;
import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebPlatformTest;
import dev.vepo.contraponto.user.User;
import jakarta.inject.Inject;

@WebPlatformTest
class NotificationNavigationTest {

    @Inject
    NotificationService notificationService;

    @Inject
    MessageComposeService messageComposeService;

    @Inject
    BlogRepository blogRepository;

    private User recipient;

    @Test
    void headerBellOpensOverlayWithoutDuplicatingMain(App app) {
        app.login(recipient)
           .clickNotificationBell()
           .assertNotificationOverlayOpen()
           .assertNotificationOverlayShows("started following")
           .assertSingleMainElement()
           .assertUrl("/");
    }

    @Test
    void messageNotificationFromInboxNavigatesToThread(App app) {
        var sender = Given.user()
                          .withUsername("navinboxsndr")
                          .withEmail("navinboxsndr@test.com")
                          .withName("Nav Inbox Sender")
                          .withPassword("password123")
                          .persist();
        var thread = messageComposeService.compose(sender.getId(),
                                                   recipient.getUsername(),
                                                   "Inbox navigation",
                                                   "Please open this thread from notifications.");
        String threadPath = MessageThreadPaths.thread(thread.getId());

        app.login(recipient)
           .openViewAllNotificationsFromOverlay()
           .clickNotificationListLinkContaining("started a message thread")
           .assertUrl(threadPath)
           .assertPageSourceContains("Inbox navigation")
           .assertPageSourceContains("hub-nav");
    }

    @Test
    void messageNotificationFromOverlayNavigatesToThread(App app) {
        var sender = Given.user()
                          .withUsername("nav-msg-sender")
                          .withEmail("nav-msg-sender@test.com")
                          .withName("Nav Msg Sender")
                          .withPassword("password123")
                          .persist();
        var thread = messageComposeService.compose(sender.getId(),
                                                   recipient.getUsername(),
                                                   "Overlay navigation",
                                                   "Please open this thread from the bell.");

        app.login(recipient)
           .visitBlog(sender.getUsername())
           .assertPageSourceDoesNotContain("/style/manage.css")
           .clickNotificationOverlayLinkContaining("started a message thread")
           .assertUrl(MessageThreadPaths.thread(thread.getId()))
           .assertManageStylesheetLoaded()
           .assertPageSourceContains("Overlay navigation")
           .assertPageSourceContains("hub-nav")
           .assertSingleMainElement();
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        var author = Given.user()
                          .withUsername("navauthor")
                          .withEmail("navauthor@test.com")
                          .withName("Nav Author")
                          .withPassword("password123")
                          .persist();
        recipient = Given.user()
                         .withUsername("navuser")
                         .withEmail("navuser@test.com")
                         .withName("Nav User")
                         .withPassword("password123")
                         .persist();
        var blog = blogRepository.findMainByOwnerId(author.getId()).orElseThrow();
        var actor = Given.user()
                         .withUsername("navactor")
                         .withEmail("navactor@test.com")
                         .withName("Nav Actor")
                         .withPassword("password123")
                         .persist();
        notificationService.notifyNewFollow(recipient, blog, actor);
    }

    @Test
    void viewAllNotificationsLinkNavigatesToInbox(App app) {
        app.login(recipient)
           .openViewAllNotificationsFromOverlay()
           .assertUrl("/account/notifications")
           .assertSingleMainElement();
    }
}
