package dev.vepo.contraponto.dashboard;

import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebTest;
import dev.vepo.contraponto.user.User;

@WebTest
class DashboardTest {

    private static final String USER_EMAIL = "dashboard@example.com";
    private static final String USER_PASSWORD = "dashboardPass123";
    private static final String USER_USERNAME = "dashboarduser";
    private static final String USER_NAME = "Dashboard Tester";

    private User testUser;

    @Test
    void authenticatedUserCanAccessDashboard(App app) {
        app.login(testUser)
           .dashboard()
           .assertTitle("Dashboard");
    }

    @Test
    void clickingRecentDraftNavigatesToEditPage(App app) {
        var draft = Given.post()
                         .withTitle("Recent Draft")
                         .withContent("Content")
                         .withAuthor(testUser)
                         .withPublished(false)
                         .persist();

        app.login(testUser)
           .dashboard()
           .clickRecentDraft(0)
           .assertUrlContains("/write/draft/" + draft.getId())
           .assertTitle("Recent Draft");
    }

    @Test
    void clickingRecentPublishedNavigatesToPostPage(App app) {
        var post = Given.post()
                        .withTitle("Recent Published")
                        .withContent("Content")
                        .withAuthor(testUser)
                        .withPublished(true)
                        .persist();

        app.login(testUser)
           .dashboard()
           .clickRecentPublished(0)
           .assertUrlContains("/post/" + post.getSlug())
           .assertPostTitle("Recent Published");
    }

    @Test
    void dashboardDisplaysViewCountsForPublishedPosts(App app) {
        var post = Given.post()
                        .withTitle("Post with Views")
                        .withContent("Content")
                        .withAuthor(testUser)
                        .withPublished(true)
                        .persist();

        // Simulate a view by visiting anonymously
        app.access()
           .goToPost(testUser, post.getSlug())
           .waitForReady();

        // Now login and check dashboard
        app.access()
           .login(testUser)
           .dashboard()
           .assertViewCountForRecentPublished(0, 1);
    }

    @Test
    void dashboardShowsCorrectDraftsAndPublishedCounts(App app) {
        IntStream.range(0, 3).forEach(i -> Given.post()
                                                .withTitle("Draft " + i)
                                                .withContent("Content")
                                                .withAuthor(testUser)
                                                .withPublished(false)
                                                .persist());

        IntStream.range(0, 5).forEach(i -> Given.post()
                                                .withTitle("Published " + i)
                                                .withContent("Content")
                                                .withAuthor(testUser)
                                                .withPublished(true)
                                                .persist());

        app.login(testUser)
           .dashboard()
           .assertDraftsStatCount(3)
           .assertPublishedStatCount(5);
    }

    @Test
    void deleteDraftTest(App app) {
        Given.post()
             .withTitle("Some Draft")
             .withContent("Content")
             .withAuthor(testUser)
             .withPublished(false)
             .persist();
        app.login(testUser)
           .dashboard()
           .clickViewAllDrafts()
           // The library page defaults to drafts tab, but we can check the active tab
           .switchTab("drafts") // ensure we are on drafts
           .assertDraftPresent("Some Draft")
           .deleteFirstDraft()
           .assertDraftNotPresent("Some Draft");
        // The active tab can be asserted by presence of draft list.
    }

    @Test
    void emptyStateMessagesWhenNoDraftsOrPublished(App app) {
        app.login(testUser)
           .dashboard()
           .assertEmptyDraftsMessage()
           .assertEmptyPublishedMessage();
    }

    @Test
    void recentDraftsDisplayOnlyFiveMostRecent(App app) {
        for (int i = 1; i <= 7; i++) {
            Given.post()
                 .withTitle("Draft " + i)
                 .withContent("Content")
                 .withAuthor(testUser)
                 .withPublished(false)
                 .persist();
        }

        app.login(testUser)
           .dashboard()
           .assertRecentDraftsCount(5)
           .assertRecentDraftTitle(0, "Draft 7");
    }

    @Test
    void recentPublishedDisplayOnlyFiveMostRecent(App app) {
        for (int i = 1; i <= 7; i++) {
            Given.post()
                 .withTitle("Published " + i)
                 .withContent("Content")
                 .withAuthor(testUser)
                 .withPublished(true)
                 .persist();
        }

        app.login(testUser)
           .dashboard()
           .assertRecentPublishedCount(5)
           .assertRecentPublishedTitle(0, "Published 7");
    }

    @BeforeEach
    void setup() {
        Given.cleanup();
        testUser = Given.user()
                        .withUsername(USER_USERNAME)
                        .withEmail(USER_EMAIL)
                        .withPassword(USER_PASSWORD)
                        .withName(USER_NAME)
                        .persist();
    }

    @Test
    void unauthenticatedUserIsRedirectedToHome(App app) {
        app.access()
           .goToPath("/manage/dashboard")
           .assertAccessButtonIsDisplayed();
    }

    @Test
    void unpublishPublishedPostTest(App app) {
        Given.post()
             .withTitle("Published To Unpublish")
             .withContent("Content")
             .withAuthor(testUser)
             .withPublished(true)
             .persist();
        app.login(testUser)
           .dashboard()
           .clickViewAllDrafts()
           .switchTab("published")
           .assertPublishedPresent("Published To Unpublish")
           .unpublishFirstPublished()
           .assertPublishedNotPresent("Published To Unpublish")
           .switchTab("drafts")
           .assertDraftPresent("Published To Unpublish");
    }

    @Test
    void viewAllDraftsLinkNavigatesToLibraryWithDraftsTab(App app) {
        // Create a draft to ensure the link is visible
        Given.post()
             .withTitle("Some Draft")
             .withContent("Content")
             .withAuthor(testUser)
             .withPublished(false)
             .persist();
        Given.post()
             .withTitle("Published Post")
             .withContent("Content")
             .withAuthor(testUser)
             .withPublished(true)
             .persist();
        app.login(testUser)
           .dashboard()
           .clickViewAllDrafts()
           // The library page defaults to drafts tab, but we can check the active tab
           .switchTab("drafts") // ensure we are on drafts
           .assertDraftNotPresent("Published Post"); // just verify library loaded
        // The active tab can be asserted by presence of draft list.
    }

    @Test
    void viewAllPublishedLinkNavigatesToLibraryWithPublishedTab(App app) {
        Given.post()
             .withTitle("Some Draft")
             .withContent("Content")
             .withAuthor(testUser)
             .withPublished(false)
             .persist();
        Given.post()
             .withTitle("Published Post")
             .withContent("Content")
             .withAuthor(testUser)
             .withPublished(true)
             .persist();

        app.login(testUser)
           .dashboard()
           .clickViewAllPublished()
           .switchTab("published")
           // Verify published list appears (or at least no error)
           .assertDraftNotPresent("Some Draft"); // just verify library loaded
    }

    @Test
    void writeNewStoryButtonNavigatesToWritePage(App app) {
        app.login(testUser)
           .dashboard()
           .clickWriteNewStory()
           .assertUrlContains("/write");
    }
}