package dev.vepo.contraponto.readinglist;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebTest;
import dev.vepo.contraponto.user.User;

@WebTest
class ReadingListWebTest {

    private static final String PASSWORD = "password123";

    private User author;
    private User reader;
    private Post post;

    @Test
    void guest_sees_sign_in_gate_on_post_page(App app) {
        app.access().goTo(post)
           .waitForReadingListAction()
           .assertReadingListSignInGateVisible();
    }

    @Test
    void mark_read_leaves_unread_tab_but_stays_on_all(App app) {
        app.login(reader)
           .goTo(post)
           .waitForReadingListAction()
           .clickReadingListSave();
        app.readingSaved()
           .assertPostTitleVisible(post.getTitle())
           .markFirstUnreadAsRead()
           .assertPostTitleAbsent(post.getTitle())
           .switchTab("all")
           .assertPostTitleVisible(post.getTitle());
    }

    @Test
    void re_save_read_item_requeues_as_unread(App app) {
        app.login(reader)
           .goTo(post)
           .waitForReadingListAction()
           .clickReadingListSave()
           .clickReadingListMarkRead()
           .clickReadingListSave()
           .assertReadingListSavedUnread();
        app.readingSaved()
           .assertPostTitleVisible(post.getTitle());
    }

    @Test
    void reading_hub_has_saved_section(App app) {
        app.login(reader)
           .openUserMenu()
           .clickMenuLink("/reading")
           .clickHubSection("/reading", "saved")
           .assertUrl("/reading/saved")
           .assertBreadcrumb("Reading", "Saved for later");
    }

    @Test
    void remove_clears_post_from_hub_and_post_page(App app) {
        app.login(reader)
           .goTo(post)
           .waitForReadingListAction()
           .clickReadingListSave()
           .clickReadingListRemove()
           .assertReadingListSaveVisible();
        app.readingSaved()
           .switchTab("all")
           .assertPostTitleAbsent(post.getTitle());
    }

    @Test
    void save_post_appears_on_unread_tab(App app) {
        app.login(reader)
           .goTo(post)
           .waitForReadingListAction()
           .clickReadingListSave()
           .assertReadingListSavedUnread();
        app.readingSaved()
           .assertPostTitleVisible(post.getTitle());
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        author = Given.user()
                      .withUsername("rlauthor")
                      .withEmail("rlauthor@test.com")
                      .withName("RL Author")
                      .withPassword(PASSWORD)
                      .persist();
        reader = Given.user()
                      .withUsername("rlreader")
                      .withEmail("rlreader@test.com")
                      .withName("RL Reader")
                      .withPassword(PASSWORD)
                      .persist();
        post = Given.post()
                    .withAuthor(author)
                    .withTitle("Reading List Target Post")
                    .withSlug("reading-list-target")
                    .withContent("Content for reading list tests.")
                    .withPublished(true)
                    .persist();
    }
}
