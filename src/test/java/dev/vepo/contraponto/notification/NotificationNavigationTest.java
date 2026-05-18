package dev.vepo.contraponto.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebTest;
import dev.vepo.contraponto.user.User;
import jakarta.inject.Inject;

@WebTest
class NotificationNavigationTest {

    @Inject
    NotificationService notificationService;

    @Inject
    BlogRepository blogRepository;

    private User recipient;

    @Test
    void headerBellAfterMenuDoesNotDuplicateMain(App app) {
        app.login(recipient)
           .openNotificationsFromMenu()
           .assertUrl("/account")
           .assertSingleMainElement()
           .clickNotificationBell()
           .assertUrl("/account/notifications")
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
}
