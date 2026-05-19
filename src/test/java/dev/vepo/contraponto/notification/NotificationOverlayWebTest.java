package dev.vepo.contraponto.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebTest;
import dev.vepo.contraponto.user.User;

@WebTest
class NotificationOverlayWebTest {

    private User userWithNoNotifications;

    @Test
    void bellOpensOverlayWithEmptyMessage(App app) {
        app.login(userWithNoNotifications)
           .clickNotificationBell()
           .assertNotificationOverlayOpen()
           .assertNotificationOverlayShows("No notification")
           .assertUrl("/");
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        userWithNoNotifications = Given.user()
                                       .withUsername("nonotify")
                                       .withEmail("nonotify@test.com")
                                       .withName("No Notify")
                                       .withPassword("password123")
                                       .persist();
    }
}
