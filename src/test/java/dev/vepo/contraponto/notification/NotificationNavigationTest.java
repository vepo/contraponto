package dev.vepo.contraponto.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.BlogRepository;
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
