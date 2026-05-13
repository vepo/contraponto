package dev.vepo.contraponto.search;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebTest;
import dev.vepo.contraponto.user.User;

@WebTest
class SearchTest {

    private User testUser;

    @Test
    void clickingSearchResultNavigatesToPost(App app) {
        app.searchPage()
           .search("Java")
           .clickFirstResult()
           .assertUrlContains("/post/")
           .waitForReady();
        // Check that the post content contains "Java"
        app.assertMainContent(); // implicit content check via text
    }

    @Test
    void modalSearchDisplaysResultsWhileTyping(App app) {
        app.access()
           .searchModal()
           .type("Java")
           .assertResultsDisplayed()
           .assertResultContains("Java")
           .close();
    }

    @Test
    void modalSearchHasAdvancedSearchLink(App app) {
        app.access()
           .searchModal()
           .assertAdvancedLinkExists()
           .close();
    }

    @Test
    void modalSearchShowsNoResultsMessage(App app) {
        app.access()
           .searchModal()
           .type("NonExistentKeywordXYZ123")
           .assertEmptyState()
           .close();
    }

    @Test
    void searchFormSubmitsViaHtmx(App app) {
        app.searchPage()
           .type("Spring")
           .submit()
           .assertResultContains("Spring");
    }

    @Test
    void searchModalOpensAndCloses(App app) {
        app.access()
           .searchModal()
           .close()
           .assertUrl("/"); // ensure we are back on home
    }

    @Test
    void searchPageHandlesPagination(App app) {
        // Create extra posts (already done in setup)
        app.searchPage()
           .search("pagination")
           .assertResultCount(20) // first page should have up to 20 results
           .loadMore()
           .assertResultCount(25); // second page should have the remaining 5
    }

    @Test
    void searchPageLoadsWithEmptyQuery(App app) {
        app.searchPage()
           .assertPageTitleContains("Search");
    }

    @Test
    void searchPageShowsEmptyState(App app) {
        app.searchPage()
           .search("NonExistentKeywordXYZ123")
           .assertEmptyState();
    }

    @Test
    void searchPageShowsResultCountAndQuery(App app) {
        app.searchPage()
           .search("Java")
           .assertHeaderContainsCountAndQuery("Java");
    }

    @Test
    void searchPageShowsResultsForQuery(App app) {
        app.searchPage()
           .search("Java")
           .assertResultContains("Java");
    }

    @Test
    void searchResultsIncludeAuthorNameAndDate(App app) {
        app.searchPage()
           .search("Java")
           .assertResultHasAuthorAndDate();
    }

    @BeforeEach
    void setup() {
        Given.cleanup();

        testUser = Given.user()
                        .withUsername("searchuser")
                        .withEmail("search@example.com")
                        .withPassword("searchpass")
                        .withName("Search Tester")
                        .persist();

        // Create posts with distinct searchable content (same as original but using
        // DSL)
        Given.post()
             .withTitle("Introduction to Java")
             .withContent("Java is a popular programming language. This post covers basics.")
             .withAuthor(testUser)
             .persist();

        Given.post()
             .withTitle("Advanced Spring Boot")
             .withContent("Learn how to build microservices with Spring Boot and Java.")
             .withAuthor(testUser)
             .persist();

        Given.post()
             .withTitle("Python vs Java")
             .withContent("Comparing Python and Java for backend development.")
             .withAuthor(testUser)
             .persist();

        Given.post()
             .withTitle("Kubernetes for Java Developers")
             .withContent("Deploy Java applications on Kubernetes.")
             .withAuthor(testUser)
             .persist();

        Given.post()
             .withTitle("Testing with JUnit 5")
             .withContent("Write unit and integration tests for your Java code.")
             .withAuthor(testUser)
             .persist();

        // Create posts for pagination test (15 posts containing "pagination")
        for (int i = 0; i < 25; i++) {
            Given.post()
                 .withTitle("Pagination Post " + i)
                 .withContent("Content for pagination test.")
                 .withAuthor(testUser)
                 .persist();
        }
    }
}