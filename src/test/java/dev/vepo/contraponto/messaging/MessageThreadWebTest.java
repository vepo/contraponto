package dev.vepo.contraponto.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebAuthTest;
import dev.vepo.contraponto.user.User;

@WebAuthTest
class MessageThreadWebTest {

    private static final String PASSWORD = "password123";

    private User sender;
    private User recipient;

    @Test
    void composeAndCloseThread(App app) {
        app.login(sender)
           .messagesCompose(recipient.getUsername())
           .fillComposeTitle("Web test thread")
           .fillComposeBody("Hello from web test")
           .submitCompose()
           .assertSingleSiteHeader()
           .assertSingleMainElement()
           .clickCloseThread()
           .assertClosedBannerVisible();
    }

    @Test
    void mailboxOpensInAccountHub(App app) {
        app.login(sender)
           .accountMessages()
           .assertUrl("/account/mailbox")
           .assertPageSourceContains("hub-layout")
           .assertPageSourceContains("/style/manage.css");
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        sender = Given.user()
                      .withUsername("wmsg-sender")
                      .withEmail("wmsg-sender@test.com")
                      .withName("Web Sender")
                      .withPassword(PASSWORD)
                      .persist();
        recipient = Given.user()
                         .withUsername("wmsg-recip")
                         .withEmail("wmsg-recip@test.com")
                         .withName("Web Recipient")
                         .withPassword(PASSWORD)
                         .persist();
    }

    @Test
    void threadAndComposeStayInsideAccountHub(App app) {
        var compose = app.login(sender).messagesCompose(recipient.getUsername());
        app.assertPageSourceContains("hub-layout").assertPageSourceContains("hub-nav");
        compose.fillComposeTitle("Hub shell thread")
               .fillComposeBody("Thread inside account hub")
               .submitCompose();
        app.assertSingleSiteHeader()
           .assertSingleMainElement()
           .assertPageSourceContains("hub-layout")
           .assertPageSourceContains("Hub shell thread");
    }
}
