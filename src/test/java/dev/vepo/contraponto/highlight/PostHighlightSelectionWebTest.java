package dev.vepo.contraponto.highlight;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebTest;
import dev.vepo.contraponto.user.User;

@WebTest
class PostHighlightSelectionWebTest {

    private static final String PASSAGE = "distributed systems";
    private static final String PASSWORD = "password123";

    private User author;
    private User reader;
    private Post post;

    @Test
    void guest_sees_sign_in_on_text_selection(App app) {
        app.access().goTo(post)
           .waitForPostHighlights()
           .selectPassageInArticle(PASSAGE)
           .assertHighlightSelectionBarShowsSignIn();
    }

    @Test
    void note_modal_opens_when_adding_note(App app) {
        app.login(reader).goTo(post)
           .waitForPostHighlights()
           .selectPassageInArticle(PASSAGE)
           .assertHighlightSelectionBarVisible()
           .clickHighlightSelectionAction("create")
           .assertPersonalHighlightMarkPresent()
           .waitForPostHighlights()
           .selectPassageInArticle(PASSAGE)
           .assertHighlightSelectionBarVisible()
           .clickHighlightSelectionAction("note")
           .assertHighlightNoteModalVisible()
           .submitHighlightNote("My private note on this passage");
    }

    @Test
    void selection_bar_appears_when_reader_selects_passage(App app) {
        var postPage = app.login(reader).goTo(post);

        postPage.waitForPostHighlights()
                .selectPassageInArticle(PASSAGE)
                .assertHighlightSelectionBarVisible()
                .clickHighlightSelectionAction("create")
                .assertPersonalHighlightMarkPresent();
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        author = Given.user()
                      .withUsername("highlightauthor")
                      .withEmail("highlightauthor@test.com")
                      .withName("Highlight Author")
                      .withPassword(PASSWORD)
                      .persist();
        reader = Given.user()
                      .withUsername("highlightreader")
                      .withEmail("highlightreader@test.com")
                      .withName("Highlight Reader")
                      .withPassword(PASSWORD)
                      .persist();
        post = Given.post()
                    .withAuthor(author)
                    .withTitle("Highlight selection test")
                    .withSlug("highlight-selection-test")
                    .withContent("""
                                 Introduction to distributed systems for readers who want to highlight passages.
                                 This paragraph gives enough text to select a meaningful snippet in the browser.
                                 """)
                    .withDescription("Highlight UI test post")
                    .withPublished(true)
                    .persist();
    }
}
