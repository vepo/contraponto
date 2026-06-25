package dev.vepo.contraponto.highlight;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebReaderTest;
import dev.vepo.contraponto.user.User;

@WebReaderTest
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
    void guest_sign_in_from_selection_bar_opens_auth_modal(App app) {
        app.access().goTo(post)
           .waitForPostHighlights()
           .selectPassageInArticle(PASSAGE)
           .assertHighlightSelectionBarShowsSignIn()
           .clickHighlightSelectionAction("sign-in")
           .assertAuthModalOpen();
    }

    @Test
    void highlight_at_paragraph_start_affects_drop_cap(App app) {
        app.login(reader).goTo(post)
           .waitForPostHighlights()
           .selectPassageInArticle("Introduction")
           .assertHighlightSelectionBarVisible()
           .clickHighlightSelectionAction("create")
           .assertPersonalHighlightMarkPresent()
           .assertHighlightAffectsDropCap();
    }

    @Test
    void note_modal_opens_from_fresh_selection(App app) {
        app.login(reader).goTo(post)
           .waitForPostHighlights()
           .selectPassageInArticle(PASSAGE)
           .assertHighlightSelectionBarVisible()
           .clickHighlightSelectionAction("note")
           .assertHighlightNoteModalVisible()
           .submitHighlightNote("Note on a fresh selection");
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
    void noted_highlight_uses_distinct_mark_style(App app) {
        app.login(reader).goTo(post)
           .waitForPostHighlights()
           .selectPassageInArticle(PASSAGE)
           .clickHighlightSelectionAction("note")
           .submitHighlightNote("Distinct noted passage")
           .waitForPostHighlights()
           .assertNotedHighlightMarkPresent();
    }

    @Test
    void remove_highlight_from_mark_click(App app) {
        app.login(reader).goTo(post)
           .waitForPostHighlights()
           .selectPassageInArticle(PASSAGE)
           .clickHighlightSelectionAction("create")
           .assertPersonalHighlightMarkPresent()
           .waitForPostHighlights()
           .clickPersonalHighlightMark()
           .clickHighlightActionBar("remove-mark")
           .assertPersonalHighlightMarkAbsent();
    }

    @Test
    void remove_note_from_note_card_click(App app) {
        app.login(reader).goTo(post)
           .waitForPostHighlights()
           .selectPassageInArticle(PASSAGE)
           .clickHighlightSelectionAction("note")
           .submitHighlightNote("Note to remove")
           .clickHighlightNoteCard("Note to remove")
           .clickHighlightActionBar("remove-note")
           .assertHighlightNoteCardAbsent("Note to remove");
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
